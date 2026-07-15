package com.aliothmoon.maameow.schedule.service
import android.content.Context
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.schedule.data.ScheduleStrategyRepository
import com.aliothmoon.maameow.schedule.model.CountdownState
import com.aliothmoon.maameow.schedule.model.ExecutionResult
import com.aliothmoon.maameow.schedule.model.ScheduledExecutionRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class ScheduledLaunchCoordinator(
    private val scope: CoroutineScope,
    private val scheduleRepository: ScheduleStrategyRepository,
    private val compositionService: MaaCompositionService,
    private val appSettingsManager: AppSettingsManager,
    private val chainState: TaskChainState,
    private val triggerLogger: ScheduleTriggerLogger,
    private val appContext: Context,
) {
    private val appCtx get() = appContext.applicationContext
    private val _countdownState = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val countdownState: StateFlow<CountdownState> = _countdownState.asStateFlow()

    private val _pendingExecution = MutableStateFlow<ScheduledExecutionRequest?>(null)
    val pendingExecution: StateFlow<ScheduledExecutionRequest?> = _pendingExecution.asStateFlow()

    private val _feedbackMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val feedbackMessages: SharedFlow<String> = _feedbackMessages.asSharedFlow()

    private var activeRequest: ScheduledExecutionRequest? = null
    private var startingRequestId: String? = null
    private var lastHandledRequestId: String? = null
    private var countdownJob: Job? = null

    fun onLaunch(request: ScheduledExecutionRequest) {
        scope.launch { handleLaunch(request) }
    }

    fun onCancel() {
        val request = activeRequest ?: return
        if (_countdownState.value !is CountdownState.Counting) return
        lastHandledRequestId = request.requestId
        triggerLogger.append("用户取消了定时任务执行")
        scope.launch {
            finishFlow(request, ExecutionResult.CANCELLED, "用户取消了定时任务执行")
        }
    }

    fun onStartNow() {
        val request = activeRequest ?: return
        if (_countdownState.value !is CountdownState.Counting) return
        triggerLogger.append("用户点击立即执行")
        promote(request)
    }

    /**
     * 由 View 在导航到后台任务页后调用。
     * [execute] 执行实际的任务启动，返回错误信息或 null 表示成功。
     */
    fun onPageReady(requestId: String, execute: suspend (ScheduledExecutionRequest) -> String?) {
        val request = _pendingExecution.value ?: return
        if (request.requestId != requestId || startingRequestId == requestId) return

        scope.launch {
            startingRequestId = requestId
            _pendingExecution.value = null
            triggerLogger.append("页面就绪，开始执行任务")
            val message = execute(request)
            if (message == null) {
                triggerLogger.append("任务启动成功")
                finishFlow(request, ExecutionResult.STARTED)
            } else {
                triggerLogger.append("任务启动失败: $message")
                finishFlow(request, ExecutionResult.FAILED_START, message)
            }
        }
    }

    fun cancel() {
        cancelCountdown()
    }

    // ==================== Internal ====================

    private suspend fun handleLaunch(request: ScheduledExecutionRequest) {
        if (isDuplicate(request)) return

        triggerLogger.append("收到启动请求: ${request.strategyName}")

if (hasPendingFlow()) {
            if (request.forceStart) {
                triggerLogger.append("强制启动: 清除当前待处理流程")
                clearFlow()
            } else {
                reject(
                    request,
                    ExecutionResult.SKIPPED_BUSY,
                    appCtx.getString(R.string.schedule_msg_busy_pending),
                )
                return
            }
        }

        if (compositionService.state.value == MaaExecutionState.RUNNING
            || compositionService.state.value == MaaExecutionState.STARTING
            || compositionService.state.value == MaaExecutionState.STOPPING
        ) {
            if (request.forceStart) {
                triggerLogger.append("强制启动: 停止当前运行任务")
                compositionService.stop()
                compositionService.stopVirtualDisplay()
            } else {
                reject(
                    request,
                    ExecutionResult.SKIPPED_BUSY,
                    appCtx.getString(R.string.schedule_msg_busy),
                )
                return
            }
        }
        val isForegroundMode = appSettingsManager.runMode.value == RunMode.FOREGROUND
        val allowForeground = appSettingsManager.allowForegroundScheduledTask.value
        if (isForegroundMode && !allowForeground) {
            reject(
                request,
                ExecutionResult.FAILED_VALIDATION,
                appCtx.getString(R.string.schedule_msg_foreground_mode_blocked),
            )
            return
        }
        triggerLogger.append("等待任务配置加载...")
        chainState.isLoaded.filter { it }.first()
        // 预检只读：真正启动时 startTasksInternal 再 switch/resolve
        val enabledNodes = if (request.useSequence) {
            val seq = chainState.sequenceConfigs.value.find { it.id == request.sequenceConfigId }
            if (seq == null) {
                reject(
                    request,
                    ExecutionResult.FAILED_VALIDATION,
                    appCtx.getString(R.string.schedule_msg_sequence_deleted),
                )
                return
            }
            triggerLogger.append(appCtx.getString(R.string.schedule_msg_using_sequence, seq.name))
            chainState.resolveExecutableChain(
                sequence = seq.entries,
                sequenceEnabled = true,
                fallbackToActive = false,
            ).filter { it.enabled }
        } else {
            val profile = chainState.profiles.value.find { it.id == request.profileId }
            if (profile == null) {
                reject(
                    request,
                    ExecutionResult.FAILED_VALIDATION,
                    appCtx.getString(R.string.schedule_msg_profile_deleted),
                )
                return
            }
            triggerLogger.append(appCtx.getString(R.string.schedule_msg_using_profile, profile.name))
            val source =
                if (request.profileId == chainState.activeProfileId.value) {
                    chainState.chain.value
                } else {
                    profile.chain
                }
            source.filter { it.enabled }
        }
        if (enabledNodes.isEmpty()) {
            reject(
                request,
                ExecutionResult.FAILED_VALIDATION,
                if (request.useSequence) {
                    appCtx.getString(R.string.schedule_msg_sequence_no_tasks)
                } else {
                    appCtx.getString(R.string.schedule_msg_profile_no_tasks)
                },
            )
            return
        }

        triggerLogger.append("前置条件通过，启用任务 ${enabledNodes.size} 项")
        lastHandledRequestId = request.requestId
        activeRequest = request
        startCountdown(request)
    }

    private fun isDuplicate(request: ScheduledExecutionRequest): Boolean {
        return request.requestId == lastHandledRequestId
                || activeRequest?.requestId == request.requestId
                || _pendingExecution.value?.requestId == request.requestId
                || startingRequestId == request.requestId
    }

    private fun hasPendingFlow(): Boolean {
        return activeRequest != null
                || _pendingExecution.value != null
                || startingRequestId != null
                || _countdownState.value is CountdownState.Counting
    }

    private fun startCountdown(request: ScheduledExecutionRequest) {
        cancelCountdown()
        triggerLogger.append("开始倒计时 (${ScheduledExecutionRequest.COUNTDOWN_SECONDS}s)")
        countdownJob = scope.launch {
            for (remaining in ScheduledExecutionRequest.COUNTDOWN_SECONDS downTo 1) {
                if (activeRequest?.requestId != request.requestId) return@launch
                _countdownState.update {
                    CountdownState.Counting(
                        strategyName = request.strategyName,
                        remainingSeconds = remaining,
                        useSequence = request.useSequence,
                    )
                }
                delay(1000)
            }
            if (activeRequest?.requestId == request.requestId) {
                triggerLogger.append("倒计时结束，提交执行")
                promote(request)
            }
        }
    }

    private fun promote(request: ScheduledExecutionRequest) {
        cancelCountdown()
        activeRequest = request
        _countdownState.value = CountdownState.Idle
        _pendingExecution.value = request
    }

    private suspend fun reject(
        request: ScheduledExecutionRequest,
        result: ExecutionResult,
        message: String,
    ) {
        lastHandledRequestId = request.requestId
        triggerLogger.append("跳过: $message")
        triggerLogger.end(result, message)
        scheduleRepository.recordExecutionResult(
            strategyId = request.strategyId,
            result = result,
            message = message,
        )
        _feedbackMessages.tryEmit(
            appCtx.getString(R.string.schedule_msg_feedback, request.strategyName, message),
        )
    }

    private suspend fun finishFlow(
        request: ScheduledExecutionRequest,
        result: ExecutionResult,
        message: String? = null,
    ) {
        try {
            triggerLogger.end(result, message)
            scheduleRepository.recordExecutionResult(
                strategyId = request.strategyId,
                result = result,
                message = message,
            )
        } finally {
            clearFlow()
        }
    }

    private fun clearFlow() {
        cancelCountdown()
        activeRequest = null
        startingRequestId = null
        _countdownState.value = CountdownState.Idle
        _pendingExecution.value = null
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }
}
