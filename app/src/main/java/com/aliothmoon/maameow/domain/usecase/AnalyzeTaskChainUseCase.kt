package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Packages
import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.model.LogLevel
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.TaskParamContext
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ResourceDataManager
import com.aliothmoon.maameow.data.resource.ServerTimezone
import com.aliothmoon.maameow.domain.models.MallCreditFightAvailability
import com.aliothmoon.maameow.domain.models.SeriesLock
import com.aliothmoon.maameow.domain.models.resolveMallCreditFightAvailability
import com.aliothmoon.maameow.maa.task.MaaTaskParams
import com.aliothmoon.maameow.utils.i18n.UiText
import com.aliothmoon.maameow.utils.i18n.uiTextOf
import timber.log.Timber
import java.time.DayOfWeek

class AnalyzeTaskChainUseCase(
    private val taskChainState: TaskChainState,
    private val resourceDataManager: ResourceDataManager,
    private val activityManager: ActivityManager,
    private val depotRepository: DepotRepository,
    private val operBoxRepository: OperBoxRepository,
    private val itemHelper: ItemHelper,
) {
    operator fun invoke(chain: List<TaskChainNode>): AnalyzeTaskChainResult {
        val enabledNodes = chain.filter { it.enabled }.sortedBy { it.order }
        if (enabledNodes.isEmpty()) {
            return AnalyzeTaskChainResult.Blocked(
                reason = AnalyzeTaskChainFailureReason.NO_TASK_SELECTED,
            )
        }

        // Split by contiguous runtime identity (client + WakeUp account tag).
        // Official(account1) → Official(account2) must be two segments, otherwise inventory shards will mix.
        val partition = partitionByContiguousClient(enabledNodes)

        val plans = partition.mapNotNull { segment -> buildPlan(segment) }
        if (plans.isEmpty()) {
            return AnalyzeTaskChainResult.Blocked(
                reason = AnalyzeTaskChainFailureReason.NO_EXECUTABLE_TASKS,
            )
        }
        return AnalyzeTaskChainResult.Ready(plans = plans)
    }

    /**
     * Split enabled nodes into contiguous runtime-identity segments.
     *
     * Rules:
     * - A new segment starts whenever WakeUp switches to a different clientType or account switch text.
     * - Interleaving is allowed (Official/1 → Bilibili/default → Official/1 → 3 segments, run in order).
     * - Grouping the same client/account together is only a UX tip (fewer switches), not a hard gate.
     * - Nodes without WakeUp inherit the current segment client/account; leading nodes before any
     *   WakeUp use [TaskChainState.getClientType] + default account.
     */
    internal fun partitionByContiguousClient(
        enabledNodes: List<TaskChainNode>,
    ): List<ClientSegment> {
        if (enabledNodes.isEmpty()) return emptyList()

        val fallbackClient = taskChainState.getClientType().ifBlank { "Official" }
        val segments = mutableListOf<ClientSegment>()
        var currentNodes = mutableListOf<TaskChainNode>()
        var currentClient: String? = null
        var currentAccountName = ""

        fun currentTag(client: String): String = depotAccountTag(client, currentAccountName)

        fun flush() {
            if (currentNodes.isEmpty()) return
            val client = currentClient ?: fallbackClient
            segments += ClientSegment(
                clientType = client,
                depotAccountTag = currentTag(client),
                nodes = currentNodes.toList(),
            )
            currentNodes = mutableListOf()
        }

        for (node in enabledNodes) {
            val wake = node.config as? WakeUpConfig
            if (wake != null) {
                val wakeClient = wake.clientType.takeIf { it.isNotBlank() } ?: currentClient ?: fallbackClient
                val wakeAccount = wake.accountName.trim()
                when {
                    currentClient == null -> {
                        currentClient = wakeClient
                        currentAccountName = wakeAccount
                    }
                    wakeClient == currentClient && wakeAccount == currentAccountName -> Unit
                    else -> {
                        flush()
                        currentClient = wakeClient
                        currentAccountName = wakeAccount
                    }
                }
            }
            currentNodes += node
        }
        flush()
        return segments
    }

    private fun buildPlan(segment: ClientSegment): TaskChainPlan? {
        val enabledNodes = segment.nodes
        val clientType = segment.clientType
        val depotAccountTag = segment.depotAccountTag
        val creditFightAvailability =
            resolveMallCreditFightAvailability(enabledNodes, activityManager)
        val serverDayOfWeek = ServerTimezone.getYjDayOfWeek(clientType)

        logCreditFightWarning(enabledNodes, creditFightAvailability)

        val ctx = TaskParamContext(
            clientType = clientType,
            depotAccountTag = depotAccountTag,
            chainAllowsCreditFight = creditFightAvailability.isAvailable,
            activityManager = activityManager,
            depotRepository = depotRepository,
            operBoxRepository = operBoxRepository,
            itemHelper = itemHelper,
            resourceDataManager = resourceDataManager,
        )

        val preflightLogs = mutableListOf<Pair<UiText, LogLevel>>()
        // TODO: MaaCore 适配代理倍率 7~10 后删除
        if (SeriesLock.isLocked(clientType) &&
            enabledNodes.any { it.config is FightConfig || it.config is DepotMaintainConfig }
        ) {
            preflightLogs += uiTextOf(R.string.runlog_series_locked) to LogLevel.WARNING
        }

        var unlockDoubleSync = false
        val params = enabledNodes.flatMap { node ->
            if (isSkippedByWeeklySchedule(node, serverDayOfWeek)) {
                return@flatMap emptyList()
            }
            val result = node.config.toTaskParams(ctx)
            preflightLogs += result.logs
            if (result.unlockDoubleSync) unlockDoubleSync = true
            result.params.map { taskParams ->
                val withNode = taskParams.copy(nodeId = node.id)
                // 理智作战的目标库存日志标签用节点名（用户可重命名），比固定 "Fight" 更可读
                val target = withNode.dropTarget
                if (node.config is FightConfig && target != null) {
                    withNode.copy(dropTarget = target.copy(logLabel = node.name))
                } else {
                    withNode
                }
            }
        }
        if (params.isEmpty()) return null

        return TaskChainPlan(
            enabledNodes = enabledNodes,
            params = params,
            clientType = clientType,
            depotAccountTag = depotAccountTag,
            gamePackageName = Packages[clientType],
            launchesGame = enabledNodes
                .mapNotNull { it.config as? WakeUpConfig }
                .any { it.startGameEnabled },
            preflightLogs = preflightLogs,
            // 双 due 标记：启动成功后 arm，两侧识别成功再解锁（见 ToolboxResultCollector）
            unlockDoubleSync = unlockDoubleSync,
        )
    }


    private fun depotAccountTag(clientType: String, accountName: String): String {
        val normalized = accountName.trim()
        if (normalized.isEmpty()) return ""
        return "$clientType:$normalized"
    }

    private fun isSkippedByWeeklySchedule(node: TaskChainNode, serverDayOfWeek: DayOfWeek): Boolean {
        val config = node.config
        if (config is FightConfig && config.useWeeklySchedule) {
            if (config.weeklySchedule[serverDayOfWeek.name] == false) {
                Timber.d("WeeklySchedule: skip node '%s' on %s", node.name, serverDayOfWeek)
                return true
            }
        }
        return false
    }

    private fun logCreditFightWarning(
        nodes: List<TaskChainNode>,
        availability: MallCreditFightAvailability,
    ) {
        if (!availability.isAvailable && nodes.any { (it.config as? MallConfig)?.creditFight == true }) {
            Timber.w(
                "Credit fight disabled because a fight task has no resolvable active stage. task=%s order=%d",
                availability.blockingTaskName ?: "unknown",
                availability.blockingTaskOrder ?: -1,
            )
        }
    }
}

/** One contiguous same-client/account slice of an enabled task chain. */
internal data class ClientSegment(
    val clientType: String,
    val depotAccountTag: String,
    val nodes: List<TaskChainNode>,
)

data class TaskChainPlan(
    val enabledNodes: List<TaskChainNode>,
    val params: List<MaaTaskParams>,
    val clientType: String,
    /** Inventory bucket tag: client + WakeUp account switch text; blank means no inventory binding. */
    val depotAccountTag: String,
    val gamePackageName: String?,
    val launchesGame: Boolean,
    val gameAliveBeforeStart: Boolean? = null,
    /**
     * 任务链分析阶段产生的、需要在会话开始后回放给用户的日志。
     * UseCase 保持无副作用，由 MaaCompositionService 在 startSession 之后统一 append。
     */
    val preflightLogs: List<Pair<UiText, LogLevel>> = emptyList(),
    /**
     * 更新数据节点同时到期干员+仓库时为 true。
     * 启动成功后 arm [com.aliothmoon.maameow.maa.callback.ToolboxResultCollector]，
     * 两侧识别成功后再报 TOOLBOX_RESULT(DepotOperBox)。
     */
    val unlockDoubleSync: Boolean = false,
)

enum class AnalyzeTaskChainFailureReason {
    NO_TASK_SELECTED,
    /** @deprecated no longer emitted — interleaving auto-splits into multiple segments. */
    INTERLEAVED_CLIENT_TYPES,
    /** @deprecated kept for string/compat; no longer emitted — multi-client chains split. */
    CONFLICTING_CLIENT_TYPES,
    NO_EXECUTABLE_TASKS,
}

sealed interface AnalyzeTaskChainResult {
    /**
     * One or more single-client plans in execution order.
     * Contiguous multi-client chains become multiple plans; callers run them sequentially.
     */
    data class Ready(val plans: List<TaskChainPlan>) : AnalyzeTaskChainResult {
        init {
            require(plans.isNotEmpty()) { "Ready.plans must not be empty" }
        }

        /** First plan (backward-compatible accessor for single-segment callers). */
        val plan: TaskChainPlan get() = plans.first()

        /**
         * True when the same clientType appears in more than one segment
         * (e.g. Official → Bilibili → Official). Not an error; soft-hint only.
         */
        val revisitsClientAcrossSegments: Boolean
            get() = plans.groupingBy { it.clientType }.eachCount().any { it.value > 1 }
    }

    data class Blocked(
        val reason: AnalyzeTaskChainFailureReason,
        val clientTypes: List<String> = emptyList(),
        /** 拦截前已产生的诊断日志（如库存保持逐条计划的失败原因），由调用方展示 */
        val preflightLogs: List<Pair<UiText, LogLevel>> = emptyList(),
    ) : AnalyzeTaskChainResult
}
