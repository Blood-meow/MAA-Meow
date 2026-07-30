package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Packages
import com.aliothmoon.maameow.data.model.CollectingPreflightLogSink
import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.model.LogLevel
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.TaskParamContext
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ResourceDataManager
import com.aliothmoon.maameow.domain.models.MallCreditFightAvailability
import com.aliothmoon.maameow.domain.models.SeriesLock
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.domain.service.FightDropsRefresher
import com.aliothmoon.maameow.maa.task.MaaTaskParams
import com.aliothmoon.maameow.maa.task.MaaTaskType
import com.aliothmoon.maameow.maa.task.TaskSlot
import com.aliothmoon.maameow.data.resource.ServerTimezone
import com.aliothmoon.maameow.utils.i18n.UiText
import com.aliothmoon.maameow.utils.i18n.uiTextOf
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.DayOfWeek

class AnalyzeTaskChainUseCase(
    private val taskChainState: TaskChainState,
    private val activityManager: ActivityManager,
    private val resourceDataManager: ResourceDataManager,
    private val depotRepository: DepotRepository,
    private val operBoxRepository: OperBoxRepository,
    private val itemHelper: ItemHelper,
    private val dropsRefresher: FightDropsRefresher,
) {
    /** 先等 depot/operBox 分片装载；config 的 toTaskParams 仍是非 suspend。 */
    suspend operator fun invoke(chain: List<TaskChainNode>): AnalyzeTaskChainResult {
        taskChainState.isLoaded.first { it }

        val enabledNodes = chain.filter { it.enabled }.sortedBy { it.order }
        if (enabledNodes.isEmpty()) {
            return AnalyzeTaskChainResult.Blocked(
                reason = AnalyzeTaskChainFailureReason.NO_TASK_SELECTED,
            )
        }

        // 每个客户端占一个连续块；账号切换不拆块，交错客户端直接拒绝。
        val partition = partitionByContiguousClient(enabledNodes)
            ?: return AnalyzeTaskChainResult.Blocked(
                reason = AnalyzeTaskChainFailureReason.INTERLEAVED_CLIENT_TYPES,
                clientTypes = enabledNodes
                    .mapNotNull { (it.config as? WakeUpConfig)?.clientType }
                    .filter { it.isNotBlank() }
                    .distinct(),
            )

        dropsRefresher.clear()
        val plans = partition.mapNotNull { segment -> buildPlan(segment) }
        if (plans.isEmpty()) {
            return AnalyzeTaskChainResult.Blocked(
                reason = AnalyzeTaskChainFailureReason.NO_EXECUTABLE_TASKS,
            )
        }
        return AnalyzeTaskChainResult.Ready(plans = plans)
    }

    /** 同客户端必须连续成块；账号切换留在该块内。 */
    internal fun partitionByContiguousClient(
        enabledNodes: List<TaskChainNode>,
    ): List<ClientSegment>? {
        if (enabledNodes.isEmpty()) return emptyList()

        val fallbackClient = taskChainState.getClientType().ifBlank { "Official" }
        val segments = mutableListOf<ClientSegment>()
        var currentNodes = mutableListOf<AccountBoundNode>()
        var currentClient: String? = null
        var currentAccountTag = ""
        val seenClients = linkedSetOf<String>()

        fun flush() {
            if (currentNodes.isEmpty()) return
            segments += ClientSegment(
                clientType = currentClient ?: fallbackClient,
                nodes = currentNodes.toList(),
            )
            currentNodes = mutableListOf()
        }

        for (node in enabledNodes) {
            val wake = node.config as? WakeUpConfig
            if (wake != null) {
                val wakeClient = wake.clientType.takeIf { it.isNotBlank() }
                    ?: currentClient
                    ?: fallbackClient
                when {
                    currentClient == null -> {
                        currentClient = wakeClient
                        seenClients += wakeClient
                    }
                    wakeClient == currentClient -> Unit
                    wakeClient in seenClients -> return null
                    else -> {
                        flush()
                        currentClient = wakeClient
                        seenClients += wakeClient
                        currentAccountTag = ""
                    }
                }
                val accountName = wake.accountName.trim()
                currentAccountTag = if (accountName.isEmpty()) {
                    currentAccountTag
                } else {
                    depotAccountTag(wakeClient, accountName)
                }
            } else if (currentClient == null) {
                currentClient = fallbackClient
                seenClients += fallbackClient
            }
            currentNodes += AccountBoundNode(node = node, accountTag = currentAccountTag)
        }
        flush()
        return segments
    }

    private fun buildPlan(segment: ClientSegment): TaskChainPlan? {
        val enabledNodes = segment.nodes.map { it.node }
        val clientType = segment.clientType
        val initialAccountTag = segment.nodes.firstOrNull()?.accountTag.orEmpty()
        val creditFightAvailability =
            MallCreditFightAvailability.resolve(enabledNodes, activityManager)
        val serverDayOfWeek = ServerTimezone.getYjDayOfWeek(clientType)
        val log = CollectingPreflightLogSink()

        // TODO: MaaCore 适配代理倍率 7~10 后删除
        if (SeriesLock.isLocked(clientType) &&
            enabledNodes.any { it.config is FightConfig || it.config is DepotMaintainConfig }
        ) {
            log.append(uiTextOf(R.string.runlog_series_locked), LogLevel.WARNING)
        }

        var unlockDoubleSync = false
        val expanded = segment.nodes.flatMap { boundNode ->
            val node = boundNode.node
            if (isSkippedByWeeklySchedule(node, serverDayOfWeek)) {
                return@flatMap emptyList()
            }
            val ctx = TaskParamContext(
                node = node,
                clientType = clientType,
                depotAccountTag = boundNode.accountTag,
                chainAllowsCreditFight = creditFightAvailability.isAvailable,
                activityManager = activityManager,
                depotRepository = depotRepository,
                operBoxRepository = operBoxRepository,
                itemHelper = itemHelper,
                resourceDataManager = resourceDataManager,
                dropsRefresher = dropsRefresher,
                logSink = log,
            )
            // UserDataUpdate 等仍可能通过扩展字段标记双 due；兼容旧返回若存在
            val rawParams = node.config.toTaskParams(ctx)
            rawParams.mapIndexed { index, task ->
                val slot = TaskSlot(
                    nodeId = node.id,
                    index = index,
                    accountTag = boundNode.accountTag.ifBlank { null },
                )
                task.copy(slot = slot)
            }
        }

        // 相邻重复 DEPOT 去重；中间有其它任务则保留
        val params = dropAdjacentDuplicateDepot(expanded)
        if (params.isEmpty()) return null

        return TaskChainPlan(
            nodes = enabledNodes,
            params = params,
            clientType = clientType,
            depotAccountTag = initialAccountTag,
            gamePackageName = Packages[clientType],
            launchesGame = enabledNodes
                .mapNotNull { it.config as? WakeUpConfig }
                .any { it.startGameEnabled },
            preflightLogs = log.entries,
            unlockDoubleSync = unlockDoubleSync,
        )
    }

    private fun dropAdjacentDuplicateDepot(params: List<MaaTaskParams>): List<MaaTaskParams> =
        params.filterIndexed { index, task ->
            index == 0 ||
                task.type != MaaTaskType.DEPOT ||
                params[index - 1].type != MaaTaskType.DEPOT
        }

    private fun depotAccountTag(clientType: String, accountName: String): String {
        val normalized = accountName.trim()
        if (normalized.isEmpty()) return ""
        return "$clientType:$normalized"
    }

    private fun isSkippedByWeeklySchedule(
        node: TaskChainNode,
        serverDayOfWeek: DayOfWeek,
    ): Boolean {
        val config = node.config
        if (config is FightConfig && config.useWeeklySchedule) {
            if (config.weeklySchedule[serverDayOfWeek.name] == false) {
                Timber.d("WeeklySchedule: skip node '%s' on %s", node.name, serverDayOfWeek)
                return true
            }
        }
        return false
    }
}

internal data class AccountBoundNode(
    val node: TaskChainNode,
    val accountTag: String,
)

internal data class ClientSegment(
    val clientType: String,
    val nodes: List<AccountBoundNode>,
)

data class TaskChainPlan(
    val nodes: List<TaskChainNode>,
    val params: List<MaaTaskParams>,
    val clientType: String,
    /** Inventory bucket tag: client + WakeUp account switch text; blank means no inventory binding. */
    val depotAccountTag: String,
    val gamePackageName: String?,
    val launchesGame: Boolean,
    val gameAliveBeforeStart: Boolean? = null,
    /** 预检日志，会话开始后由 Composition 回放。 */
    val preflightLogs: List<Pair<UiText, LogLevel>> = emptyList(),
    /** 更新数据节点同时到期干员+仓库时为 true。 */
    val unlockDoubleSync: Boolean = false,
) {
    /** 兼容上游 logs 命名。 */
    val logs: List<Pair<UiText, LogLevel>> get() = preflightLogs
    val enabledNodes: List<TaskChainNode> get() = nodes
}

enum class AnalyzeTaskChainFailureReason {
    NO_TASK_SELECTED,
    /** A client appears again after a different client block; reorder the chain before starting. */
    INTERLEAVED_CLIENT_TYPES,
    /** @deprecated kept for serialized/string compatibility. */
    CONFLICTING_CLIENT_TYPES,
    NO_EXECUTABLE_TASKS,
}

sealed interface AnalyzeTaskChainResult {
    /**
     * One or more contiguous single-client plans in execution order.
     * Each client may occupy only one block; callers run valid blocks sequentially.
     */
    data class Ready(val plans: List<TaskChainPlan>) : AnalyzeTaskChainResult {
        /** Convenience for single-plan callers. */
        val plan: TaskChainPlan get() = plans.first()
    }

    data class Blocked(
        val reason: AnalyzeTaskChainFailureReason,
        val clientTypes: List<String> = emptyList(),
        val preflightLogs: List<Pair<UiText, LogLevel>> = emptyList(),
    ) : AnalyzeTaskChainResult {
        val logs: List<Pair<UiText, LogLevel>> get() = preflightLogs
    }
}
