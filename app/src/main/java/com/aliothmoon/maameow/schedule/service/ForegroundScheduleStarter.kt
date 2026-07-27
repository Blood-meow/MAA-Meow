package com.aliothmoon.maameow.schedule.service

import android.content.Context
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.TaskChainNode
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.OverlayControlMode
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.domain.service.GameMuteCoordinator
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.domain.usecase.PrepareTaskStartUseCase
import com.aliothmoon.maameow.domain.usecase.TaskStartContext
import com.aliothmoon.maameow.domain.usecase.TaskStartDecision
import com.aliothmoon.maameow.domain.usecase.TaskStartMode
import com.aliothmoon.maameow.overlay.OverlayController
import com.aliothmoon.maameow.schedule.data.ScheduleStrategyRepository
import com.aliothmoon.maameow.schedule.model.CountdownState
import com.aliothmoon.maameow.schedule.model.ExecutionResult
import com.aliothmoon.maameow.schedule.model.ScheduledExecutionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class ForegroundScheduleStarter(
    private val appContext: Context,
    private val overlayController: OverlayController,
    private val prepareTaskStartUseCase: PrepareTaskStartUseCase,
    private val chainState: TaskChainState,
    private val compositionService: MaaCompositionService,
    private val triggerLogger: ScheduleTriggerLogger,
    private val scheduleRepository: ScheduleStrategyRepository,
    private val appSettingsManager: AppSettingsManager,
    private val gameMuteCoordinator: GameMuteCoordinator,
) {
    private val appCtx get() = appContext.applicationContext
    private val executing = AtomicBoolean(false)

    suspend fun executeSilentStart(request: ScheduledExecutionRequest) {
        if (!executing.compareAndSet(false, true)) {
            val busy = appCtx.getString(R.string.schedule_msg_busy_other)
            triggerLogger.append(busy)
            recordResult(request, ExecutionResult.SKIPPED_BUSY, busy)
            return
        }
        try {
            Timber.i("接管前台定时请求 ${request.requestId}")

            if (compositionService.state.value == MaaExecutionState.RUNNING ||
                compositionService.state.value == MaaExecutionState.STARTING) {
                if (request.forceStart) {
                    triggerLogger.append("强制启动: 停止当前运行任务")
                    compositionService.stop()
                    compositionService.stopVirtualDisplay()
                } else {
                    val busyMsg = appCtx.getString(R.string.schedule_msg_busy_skip)
                    triggerLogger.append(busyMsg)
                    recordResult(request, ExecutionResult.SKIPPED_BUSY, busyMsg)
                    return
                }
            }

            chainState.isLoaded.first { it }
            // 倒计时前先做一轮校验，失败则不必等 30s；真正启动前再 re-resolve，
            // 避免倒计时窗口内改配置/改链仍跑旧快照。
            when (val pre = resolveEnabledChain(request)) {
                is ResolveChainResult.Failed -> {
                    triggerLogger.append(pre.message)
                    recordResult(request, ExecutionResult.FAILED_VALIDATION, pre.message)
                    return
                }
                is ResolveChainResult.Ok -> {
                    triggerLogger.append(pre.logLine)
                }
            }
            // 无论哪种模式，都执行倒计时等待到整点
            var isStartingNow = false
            val isFloatBall = appSettingsManager.overlayControlMode.value == OverlayControlMode.FLOAT_BALL
            if (isFloatBall) {
                triggerLogger.append("开始倒计时 (${ScheduledExecutionRequest.COUNTDOWN_SECONDS}s)")
                try {
                    overlayController.setTemporaryCountdownListener {
                        isStartingNow = true
                        triggerLogger.append("用户点击立即执行")
                    }
                    for (remaining in ScheduledExecutionRequest.COUNTDOWN_SECONDS downTo 1) {
                        if (isStartingNow) break
                        overlayController.updateCountdownState(
                            CountdownState.Counting(
                                strategyName = request.strategyName,
                                remainingSeconds = remaining,
                                useSequence = request.useSequence,
                            )
                        )
                        delay(1000)
                    }
                } finally {
                    overlayController.updateCountdownState(CountdownState.Idle)
                    overlayController.setTemporaryCountdownListener(null)
                }
            } else {
                // 非悬浮球模式：静默等待，不更新 UI（但仍保证任务在整点开始）
                triggerLogger.append("非悬浮球模式，静默倒计时")
                delay(ScheduledExecutionRequest.COUNTDOWN_SECONDS * 1000L)
            }
            triggerLogger.append("倒计时结束，开始准备执行")
            val chain = when (val resolved = resolveEnabledChain(request)) {
                is ResolveChainResult.Failed -> {
                    triggerLogger.append(resolved.message)
                    recordResult(request, ExecutionResult.FAILED_VALIDATION, resolved.message)
                    return
                }
                is ResolveChainResult.Ok -> {
                    if (resolved.logLine.isNotEmpty()) {
                        triggerLogger.append("重新解析: ${resolved.logLine}")
                    }
                    resolved.chain
                }
            }
            if (chain.isEmpty()) {
                val emptyMsg = if (request.useSequence) {
                    appCtx.getString(R.string.schedule_msg_sequence_no_tasks)
                } else {
                    appCtx.getString(R.string.schedule_msg_profile_no_tasks)
                }
                triggerLogger.append(emptyMsg)
                recordResult(request, ExecutionResult.FAILED_VALIDATION, emptyMsg)
                return
            }

            // Align with BackgroundTaskViewModel.doSwitchProfile: PROFILE schedules activate
            // the target config so UI state matches execution. SEQUENCE keeps current active.
            // Pre-check stays read-only; switch only happens on the real start path.
            if (!request.useSequence && request.profileId.isNotEmpty() &&
                chainState.activeProfileId.value != request.profileId
            ) {
                chainState.switchProfile(request.profileId)
            }

            try {
                val startContext = TaskStartContext(mode = TaskStartMode.SCHEDULED)
                when (val decision = prepareTaskStartUseCase.invoke(chain, startContext)) {
                    is TaskStartDecision.Ready -> {
                        val plans = decision.plans
                        if (plans.size > 1) {
                            triggerLogger.append(
                                appCtx.getString(
                                    R.string.runlog_client_segments_plan,
                                    plans.size,
                                    plans.joinToString { it.clientType },
                                ),
                            )
                            if (plans.groupingBy { it.clientType }.eachCount().any { it.value > 1 }) {
                                triggerLogger.append(
                                    appCtx.getString(R.string.runlog_client_segments_revisit_hint),
                                )
                            }
                        }
                        triggerLogger.append(
                            appCtx.getString(R.string.runlog_task_start, chain.size, plans.first().clientType),
                        )

                        var failed: String? = null
                        for ((index, plan) in plans.withIndex()) {
                            if (index > 0) {
                                triggerLogger.append(
                                    appCtx.getString(
                                        R.string.runlog_client_segment_next,
                                        index,
                                        plan.clientType,
                                    ),
                                )
                                runCatching { compositionService.stopVirtualDisplay() }
                            }
                            val muteRequested = appSettingsManager.muteOnGameLaunch.value
                            if (muteRequested && !gameMuteCoordinator.mute(plan.clientType)) {
                                triggerLogger.append(appCtx.getString(R.string.bg_toast_mute_failed))
                            }
                            val result = compositionService.start(
                                tasks = plan.params,
                                clientType = plan.clientType,
                                isScheduled = true,
                                preflightLogs = plan.preflightLogs,
                                expectDoubleSync = plan.unlockDoubleSync,
                            )
                            if (result !is MaaCompositionService.StartResult.Success) {
                                failed = appCtx.getString(
                                    R.string.runlog_client_segment_aborted,
                                    "${plan.clientType}: $result",
                                )
                                triggerLogger.append(failed)
                                break
                            }
                            triggerLogger.append(
                                appCtx.getString(
                                    R.string.runlog_client_segment_start,
                                    index + 1,
                                    plans.size,
                                    plan.clientType,
                                    plan.params.size,
                                ) + " (v${result.version})",
                            )
                            if (index < plans.lastIndex) {
                                // Wait for this segment to finish before switching client.
                                compositionService.state.first { it == MaaExecutionState.RUNNING }
                                val endState = compositionService.state.first {
                                    it == MaaExecutionState.IDLE || it == MaaExecutionState.ERROR
                                }
                                if (endState == MaaExecutionState.ERROR) {
                                    failed = appCtx.getString(
                                        R.string.runlog_client_segment_aborted,
                                        "${plan.clientType} ERROR",
                                    )
                                    triggerLogger.append(failed)
                                    break
                                }
                            }
                        }
                        if (failed == null) {
                            if (plans.size > 1) {
                                triggerLogger.append(appCtx.getString(R.string.runlog_client_segment_done_all))
                            }
                            recordResult(request, ExecutionResult.STARTED)
                        } else {
                            recordResult(request, ExecutionResult.FAILED_START, failed)
                        }
                    }
                    is TaskStartDecision.Blocked -> {
                        val blockMsg = "任务被拦截，原因: ${decision.reason}"
                        triggerLogger.append(blockMsg)
                        recordResult(request, ExecutionResult.FAILED_VALIDATION, blockMsg)
                    }
                    // 不可达分支：定时入口固定 TaskStartMode.SCHEDULED，闸门只会产出 Ready/Blocked，
                    // RequiresConfirmation 仅在 MANUAL 模式产生。仅为 when 兜底，无需处理。
                    else -> Unit
                }
            } catch (e: Exception) {
                val errMsg = "解析任务并启动时发生异常: ${e.message}"
                triggerLogger.append(errMsg)
                recordResult(request, ExecutionResult.FAILED_START, errMsg)
            }
        } finally {
            executing.set(false)
        }
    }

    /**
     * 解析当前时刻可执行的启用节点列表。
     * 倒计时前后各调一次，避免 30s 窗口内配置变更导致跑旧快照。
     * 配置缺失或无可执行节点均返回 Failed（空链不进入倒计时）。
     */
    private suspend fun resolveEnabledChain(
        request: ScheduledExecutionRequest,
    ): ResolveChainResult {
        return if (request.useSequence) {
            val seq = chainState.sequenceConfigs.value.find { it.id == request.sequenceConfigId }
            if (seq == null) {
                ResolveChainResult.Failed(appCtx.getString(R.string.schedule_msg_sequence_deleted))
            } else {
                val chain = chainState.resolveExecutableChain(
                    sequence = seq.entries,
                    sequenceEnabled = true,
                    fallbackToActive = false,
                ).filter { it.enabled }
                if (chain.isEmpty()) {
                    ResolveChainResult.Failed(appCtx.getString(R.string.schedule_msg_sequence_no_tasks))
                } else {
                    ResolveChainResult.Ok(
                        chain = chain,
                        logLine = appCtx.getString(R.string.schedule_msg_using_sequence, seq.name),
                    )
                }
            }
        } else {
            // 预检只读：不 switch，避免失败路径改写 active profile
            val profile = chainState.profiles.value.find { it.id == request.profileId }
            if (profile == null) {
                ResolveChainResult.Failed(appCtx.getString(R.string.schedule_msg_profile_deleted))
            } else {
                val sourceChain =
                    if (request.profileId == chainState.activeProfileId.value) {
                        chainState.chain.value
                    } else {
                        profile.chain
                    }
                val chain = sourceChain.filter { it.enabled }
                if (chain.isEmpty()) {
                    ResolveChainResult.Failed(appCtx.getString(R.string.schedule_msg_profile_no_tasks))
                } else {
                    ResolveChainResult.Ok(
                        chain = chain,
                        logLine = appCtx.getString(R.string.schedule_msg_using_profile, profile.name),
                    )
                }
            }
        }
    }

    private sealed class ResolveChainResult {
        data class Ok(
            val chain: List<TaskChainNode>,
            val logLine: String,
        ) : ResolveChainResult()

        data class Failed(val message: String) : ResolveChainResult()
    }

    /**
     * 向日志器和数据库同时写入最终状态，闭合日志会话
     */
    private suspend fun recordResult(
        request: ScheduledExecutionRequest,
        result: ExecutionResult,
        message: String? = null
    ) {
        triggerLogger.end(result, message)
        scheduleRepository.recordExecutionResult(
            strategyId = request.strategyId,
            result = result,
            message = message
        )
    }
}
