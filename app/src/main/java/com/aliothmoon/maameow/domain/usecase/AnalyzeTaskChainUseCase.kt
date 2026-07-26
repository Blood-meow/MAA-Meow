package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Packages
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.model.LogLevel
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.ReclamationConfig
import com.aliothmoon.maameow.data.model.RoguelikeConfig
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.model.WakeUpConfig
import com.aliothmoon.maameow.data.preferences.TaskChainState
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
) {
    operator fun invoke(chain: List<TaskChainNode>): AnalyzeTaskChainResult {
        val enabledNodes = chain.filter { it.enabled }.sortedBy { it.order }
        if (enabledNodes.isEmpty()) {
            return AnalyzeTaskChainResult.Blocked(
                reason = AnalyzeTaskChainFailureReason.NO_TASK_SELECTED,
            )
        }

        // Always split by contiguous same-client runs (Official→Bilibili→Official = 3 segments).
        // Interleaving is allowed; callers may soft-hint that grouping same clients reduces switches.
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
     * Split enabled nodes into contiguous same-client segments.
     *
     * Rules:
     * - A new segment starts whenever WakeUp switches to a different clientType.
     * - Interleaving is allowed (Official → Bilibili → Official → 3 segments, run in order).
     * - Grouping the same client together is only a UX tip (fewer client switches), not a hard gate.
     * - Nodes without WakeUp inherit the current segment client; leading nodes before any
     *   WakeUp use [TaskChainState.getClientType] as the initial client.
     */
    internal fun partitionByContiguousClient(
        enabledNodes: List<TaskChainNode>,
    ): List<ClientSegment> {
        if (enabledNodes.isEmpty()) return emptyList()

        val fallbackClient = taskChainState.getClientType().ifBlank { "Official" }
        val segments = mutableListOf<ClientSegment>()
        var currentNodes = mutableListOf<TaskChainNode>()
        var currentClient: String? = null

        fun flush() {
            if (currentNodes.isEmpty()) return
            val client = currentClient ?: fallbackClient
            segments += ClientSegment(clientType = client, nodes = currentNodes.toList())
            currentNodes = mutableListOf()
        }

        for (node in enabledNodes) {
            val wakeClient = (node.config as? WakeUpConfig)?.clientType
                ?.takeIf { it.isNotBlank() }
            if (wakeClient != null) {
                when {
                    currentClient == null -> currentClient = wakeClient
                    wakeClient == currentClient -> Unit
                    else -> {
                        flush()
                        currentClient = wakeClient
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
        val creditFightAvailability = resolveMallCreditFightAvailability(enabledNodes)
        val serverDayOfWeek = ServerTimezone.getYjDayOfWeek(clientType)

        logCreditFightWarning(enabledNodes, creditFightAvailability)

        val params = enabledNodes.mapNotNull { node ->
            if (isSkippedByWeeklySchedule(node, serverDayOfWeek)) {
                return@mapNotNull null
            }
            buildNodeParams(node, creditFightAvailability, clientType)
        }
        if (params.isEmpty()) return null

        val preflightLogs = mutableListOf<Pair<UiText, LogLevel>>()
        // TODO: MaaCore 适配代理倍率 7~10 后删除
        if (SeriesLock.isLocked(clientType) && enabledNodes.any { it.config is FightConfig }) {
            preflightLogs += uiTextOf(R.string.runlog_series_locked) to LogLevel.WARNING
        }

        return TaskChainPlan(
            enabledNodes = enabledNodes,
            params = params,
            clientType = clientType,
            gamePackageName = Packages[clientType],
            launchesGame = enabledNodes
                .mapNotNull { it.config as? WakeUpConfig }
                .any { it.startGameEnabled },
            preflightLogs = preflightLogs,
        )
    }

    private fun isSkippedByWeeklySchedule(node: TaskChainNode, serverDayOfWeek: DayOfWeek): Boolean {
        val config = node.config
        if (config is FightConfig && config.useWeeklySchedule) {
            if (config.weeklySchedule[serverDayOfWeek.name] == false) {
                Timber.d("WeeklySchedule: skip node \'%s\' on %s", node.name, serverDayOfWeek)
                return true
            }
        }
        return false
    }

    private fun buildNodeParams(
        node: TaskChainNode,
        creditFightAvailability: MallCreditFightAvailability,
        clientType: String,
    ): MaaTaskParams {
        val base = when (val config = node.config) {
            is MallConfig -> {
                config.toTaskParams(
                    creditFightEnabled = config.creditFight && creditFightAvailability.isAvailable,
                    clientType = clientType,
                )
            }

            is ReclamationConfig -> config.toTaskParams(clientType = clientType)

            is RoguelikeConfig -> config.toTaskParams { coreChar ->
                resourceDataManager.getCharacterByNameOrAlias(coreChar)?.name ?: coreChar
            }

            is FightConfig -> config.toTaskParams(clientType = clientType)

            else -> node.config.toTaskParams()
        }
        return base.copy(nodeId = node.id)
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

/** One contiguous same-client slice of an enabled task chain. */
internal data class ClientSegment(
    val clientType: String,
    val nodes: List<TaskChainNode>,
)

data class TaskChainPlan(
    val enabledNodes: List<TaskChainNode>,
    val params: List<MaaTaskParams>,
    val clientType: String,
    val gamePackageName: String?,
    val launchesGame: Boolean,
    val gameAliveBeforeStart: Boolean? = null,
    /**
     * 任务链分析阶段产生的、需要在会话开始后回放给用户的日志。
     * UseCase 保持无副作用，由 MaaCompositionService 在 startSession 之后统一 append。
     */
    val preflightLogs: List<Pair<UiText, LogLevel>> = emptyList(),
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
    ) : AnalyzeTaskChainResult
}
