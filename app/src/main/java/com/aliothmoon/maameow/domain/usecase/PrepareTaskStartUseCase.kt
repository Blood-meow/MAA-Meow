package com.aliothmoon.maameow.domain.usecase

import com.aliothmoon.maameow.data.model.TaskChainNode

/**
 * 主任务链的启动决策：链分析([AnalyzeTaskChainUseCase]) + 游戏就绪性闸门([CheckGameReadinessUseCase])。
 *
 * 多客户端且按相同客户端连续分组时，分析会产出多个 [TaskChainPlan]；本用例只对**第一段**
 * 做就绪检查，后续段在上一段自然结束后再启动并由调用方再次校验。
 */
class PrepareTaskStartUseCase(
    private val analyzeTaskChainUseCase: AnalyzeTaskChainUseCase,
    private val checkGameReadiness: CheckGameReadinessUseCase,
) {
    suspend operator fun invoke(
        chain: List<TaskChainNode>,
        context: TaskStartContext,
    ): TaskStartDecision {
        val plans = when (val analyzeResult = analyzeTaskChainUseCase(chain)) {
            is AnalyzeTaskChainResult.Ready -> analyzeResult.plans
            is AnalyzeTaskChainResult.Blocked -> {
                return TaskStartDecision.Blocked(
                    reason = analyzeResult.reason.toDecisionReason(),
                    clientTypes = analyzeResult.clientTypes,
                )
            }
        }

        val first = plans.first()
        return when (val readiness = checkGameReadiness(first.clientType, first.launchesGame, context)) {
            is GameReadiness.Ready ->
                TaskStartDecision.Ready(
                    plans = listOf(
                        first.copy(gameAliveBeforeStart = readiness.gameAliveBeforeStart),
                    ) + plans.drop(1),
                )

            is GameReadiness.RequiresConfirmation ->
                TaskStartDecision.RequiresConfirmation(readiness.acknowledgement)

            is GameReadiness.Blocked ->
                TaskStartDecision.Blocked(readiness.reason)
        }
    }
}

data class TaskStartContext(
    val mode: TaskStartMode,
    val acknowledgements: Set<TaskStartAcknowledgement> = emptySet(),
) {
    fun acknowledged(acknowledgement: TaskStartAcknowledgement): TaskStartContext {
        return copy(acknowledgements = acknowledgements + acknowledgement)
    }
}

enum class TaskStartMode {
    MANUAL,
    SCHEDULED,
}

enum class TaskStartAcknowledgement {
    GAME_NOT_RUNNING_WITHOUT_WAKE_UP,
    GAME_NOT_INSTALLED,
}

enum class TaskStartDecisionReason {
    NO_TASK_SELECTED,
    /** @deprecated no longer emitted — interleaving auto-splits into multiple segments. */
    INTERLEAVED_CLIENT_TYPES,
    /** @deprecated no longer emitted by analyze after auto-split. */
    CONFLICTING_CLIENT_TYPES,
    NO_EXECUTABLE_TASKS,
    GAME_NOT_RUNNING_WITHOUT_WAKE_UP,
    GAME_NOT_INSTALLED,
    GAME_NOT_ON_BACKGROUND_DISPLAY,
}

sealed interface TaskStartDecision {
    data class Ready(val plans: List<TaskChainPlan>) : TaskStartDecision {
        init {
            require(plans.isNotEmpty()) { "Ready.plans must not be empty" }
        }

        val plan: TaskChainPlan get() = plans.first()
    }

    data class RequiresConfirmation(
        val acknowledgement: TaskStartAcknowledgement,
    ) : TaskStartDecision

    data class Blocked(
        val reason: TaskStartDecisionReason,
        val clientTypes: List<String> = emptyList(),
    ) : TaskStartDecision
}

private fun AnalyzeTaskChainFailureReason.toDecisionReason(): TaskStartDecisionReason {
    return when (this) {
        AnalyzeTaskChainFailureReason.NO_TASK_SELECTED -> TaskStartDecisionReason.NO_TASK_SELECTED
        AnalyzeTaskChainFailureReason.INTERLEAVED_CLIENT_TYPES ->
            TaskStartDecisionReason.INTERLEAVED_CLIENT_TYPES
        AnalyzeTaskChainFailureReason.CONFLICTING_CLIENT_TYPES ->
            TaskStartDecisionReason.CONFLICTING_CLIENT_TYPES
        AnalyzeTaskChainFailureReason.NO_EXECUTABLE_TASKS -> TaskStartDecisionReason.NO_EXECUTABLE_TASKS
    }
}
