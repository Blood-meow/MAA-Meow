package com.aliothmoon.maameow.presentation.viewmodel

import android.content.Context
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.RemoteService
import com.aliothmoon.maameow.data.config.MaaPathConfig
import com.aliothmoon.maameow.data.model.LogItem
import com.aliothmoon.maameow.data.model.TaskParamProvider
import com.aliothmoon.maameow.data.model.TaskTypeInfo
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.domain.service.GameMuteCoordinator
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.domain.service.MaaSessionLogger
import com.aliothmoon.maameow.domain.service.AchievementReporter
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.domain.usecase.PrepareTaskStartUseCase
import com.aliothmoon.maameow.domain.usecase.TaskChainPlan
import com.aliothmoon.maameow.domain.usecase.TaskStartContext
import com.aliothmoon.maameow.domain.usecase.TaskStartDecision
import com.aliothmoon.maameow.domain.usecase.TaskStartMode
import com.aliothmoon.maameow.manager.RemoteServiceManager
import com.aliothmoon.maameow.presentation.state.BackgroundTaskState
import com.aliothmoon.maameow.presentation.state.PreviewTouchMarker
import com.aliothmoon.maameow.presentation.state.UiEffect
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogConfirmAction
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogUiState
import com.aliothmoon.maameow.presentation.view.panel.PanelTab
import com.aliothmoon.maameow.schedule.data.ScheduleStrategyRepository
import com.aliothmoon.maameow.schedule.model.ScheduledExecutionRequest
import com.aliothmoon.maameow.schedule.service.ScheduleAlarmManager
import com.aliothmoon.maameow.schedule.service.ScheduleTriggerLogger
import com.aliothmoon.maameow.schedule.service.ScheduledLaunchCoordinator
import com.aliothmoon.maameow.utils.i18n.UiText
import com.aliothmoon.maameow.utils.i18n.resolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class BackgroundTaskViewModel(
    val chainState: TaskChainState,
    private val prepareTaskStart: PrepareTaskStartUseCase,
    private val compositionService: MaaCompositionService,
    private val sessionLogger: MaaSessionLogger,
    private val appSettingsManager: AppSettingsManager,
    private val pathConfig: MaaPathConfig,
    private val achievementReporter: AchievementReporter,
    private val gameMuteCoordinator: GameMuteCoordinator,
    private val scheduleRepository: ScheduleStrategyRepository,
    private val scheduleAlarmManager: ScheduleAlarmManager,
    triggerLogger: ScheduleTriggerLogger,
    private val application: Context,
) : ViewModel() {
    val coordinator = ScheduledLaunchCoordinator(
        scope = viewModelScope,
        scheduleRepository = scheduleRepository,
        compositionService = compositionService,
        appSettingsManager = appSettingsManager,
        chainState = chainState,
        triggerLogger = triggerLogger,
        appContext = application,
    )

    private val _state = MutableStateFlow(BackgroundTaskState())
    val state: StateFlow<BackgroundTaskState> = _state.asStateFlow()
    val logs: StateFlow<List<LogItem>> = sessionLogger.logs

    private val surfaceRef = AtomicReference<Surface>()

    val isGameMuted: StateFlow<Boolean> = gameMuteCoordinator.isMuted

    // 调试截图结果（已本地化的提示文案），供 UI 以 Toast 展示
    private val _screenshotMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val screenshotMessage: SharedFlow<String> = _screenshotMessage.asSharedFlow()

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects: Flow<UiEffect> = _effects.receiveAsFlow()

    private val touchPreviewController = TouchPreviewController(viewModelScope)
    val markers: StateFlow<List<PreviewTouchMarker>> = touchPreviewController.markers
    private var pendingStart: PendingStart? = null

    /** Remaining single-client plans after the current segment (contiguous multi-client split). */
    private var pendingClientPlans: List<TaskChainPlan> = emptyList()

    /** Total segments for the active multi-client run (for run-log ordinals). */
    private var clientSegmentTotal: Int = 0

    private data class PendingStart(
        val context: TaskStartContext,
        val request: ScheduledExecutionRequest? = null,
    )

    init {
        Timber.i("BackgroundTaskViewModel inited")
        observeServiceState()
        observeTaskEnd()
        observeTouchPreviewToggle()
    }

    private fun observeTouchPreviewToggle() {
        viewModelScope.launch {
            appSettingsManager.showTouchPreview.collect { enabled ->
                touchPreviewController.onTouchCallbackChange(enabled)
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            RemoteServiceManager.state
                .drop(1)
                .collect { state ->
                    when (state) {
                        // 服务重连
                        is RemoteServiceManager.ServiceState.Connected -> {
                            onServiceReconnected(state.service)
                        }

                        is RemoteServiceManager.ServiceState.Error -> {
                            touchPreviewController.onClear()
                        }

                        else -> Unit
                    }
                }
        }
    }

    fun onServiceReconnected(srv: RemoteService) {
        if (surfaceRef.get() != null) {
            onMonitorSurfaceChanged(srv)
        }
        val enabled = appSettingsManager.showTouchPreview.value
        touchPreviewController.onTouchCallbackChange(enabled)
    }

    private fun observeTaskEnd() {
        viewModelScope.launch {
            var prev = compositionService.state.value
            compositionService.state.collect { current ->
                // Clear queued multi-client segments on user/external stop so a conflated
                // RUNNING→IDLE (skipped STOPPING) cannot auto-start the next segment.
                // Covers float-ball / volume-key stops that only call compositionService.stop().
                if (current == MaaExecutionState.STOPPING) {
                    pendingClientPlans = emptyList()
                    clientSegmentTotal = 0
                }
                val naturalEnd = prev == MaaExecutionState.RUNNING &&
                    (current == MaaExecutionState.IDLE || current == MaaExecutionState.ERROR)
                // 手动停止走 RUNNING → STOPPING → IDLE，prev 为 STOPPING 不会匹配。
                if (naturalEnd) {
                    val remaining = pendingClientPlans
                    if (remaining.isNotEmpty() && current == MaaExecutionState.IDLE) {
                        // Contiguous multi-client: start next single-client segment.
                        pendingClientPlans = emptyList()
                        viewModelScope.launch {
                            continueClientSegmentQueue(remaining)
                        }
                    } else if (remaining.isNotEmpty() && current == MaaExecutionState.ERROR) {
                        Timber.w("Segment ended in ERROR; dropping %d remaining client segment(s)", remaining.size)
                        pendingClientPlans = emptyList()
                        clientSegmentTotal = 0
                        viewModelScope.launch {
                            sessionLogger.appendAndWait(
                                application.getString(
                                    R.string.runlog_client_segment_aborted,
                                    current.name,
                                ),
                            )
                        }
                    } else if (appSettingsManager.closeAppOnTaskEnd.value) {
                        // 仅在任务自然结束且无后续客户端段时关闭游戏
                        Timber.i("Task ended (%s), auto closing app", current)
                        _effects.send(UiEffect.toast(R.string.bg_toast_auto_closed_on_end))
                        compositionService.stopVirtualDisplay()
                    }
                }
                prev = current
            }
        }
    }

    /** Run the next queued single-client plan after a natural IDLE end. */
    private suspend fun continueClientSegmentQueue(remaining: List<TaskChainPlan>) {
        val next = remaining.firstOrNull() ?: return
        val rest = remaining.drop(1)
        Timber.i(
            "Starting next client segment %s (%d task(s)); %d segment(s) after this",
            next.clientType,
            next.params.size,
            rest.size,
        )
        val finishedSegment = (clientSegmentTotal - remaining.size).coerceAtLeast(1)
        val segmentIndex = (finishedSegment + 1).coerceAtMost(clientSegmentTotal.coerceAtLeast(1))
        sessionLogger.appendAndWait(
            application.getString(
                R.string.runlog_client_segment_next,
                finishedSegment,
                next.clientType,
            ),
        )
        _effects.send(
            UiEffect.toast(
                application.getString(
                    R.string.task_start_toast_next_client_segment,
                    next.clientType,
                ),
            ),
        )
        runCatching { compositionService.stopVirtualDisplay() }
            .onFailure { Timber.w(it, "stopVirtualDisplay before next client segment failed") }

        val muteRequested = appSettingsManager.muteOnGameLaunch.value
        if (muteRequested && !gameMuteCoordinator.mute(next.clientType)) {
            _effects.send(UiEffect.toast(R.string.bg_toast_mute_failed))
        }

        val result = compositionService.start(
            tasks = next.params,
            clientType = next.clientType,
            depotAccountTag = next.depotAccountTag,
            isScheduled = false,
            preflightLogs = next.preflightLogs,
            expectDoubleSync = next.unlockDoubleSync,
        ) {
            sessionLogger.appendAndWait(
                application.getString(
                    R.string.runlog_client_segment_start,
                    segmentIndex,
                    clientSegmentTotal,
                    next.clientType,
                    next.params.size,
                ),
            )
        }
        if (result is MaaCompositionService.StartResult.Success) {
            pendingClientPlans = rest
            chainState.grantGameBatteryExemption(next.clientType)
            achievementReporter.reportTaskStarted(
                taskCount = next.params.size,
                launchesGame = next.launchesGame,
                gameAliveBeforeStart = next.gameAliveBeforeStart,
            )
            if (rest.isEmpty()) {
                sessionLogger.appendAndWait(
                    application.getString(R.string.runlog_client_segment_done_all),
                )
                clientSegmentTotal = 0
            }
        } else {
            pendingClientPlans = emptyList()
            clientSegmentTotal = 0
            val message = application.resolveTaskStartFailureMessage(result)
            sessionLogger.appendAndWait(
                application.getString(
                    R.string.runlog_client_segment_aborted,
                    message?.resolve(application) ?: result.toString(),
                ),
            )
            if (message != null) {
                Timber.w("Next client segment start failed: %s", message.resolve(application))
                showStartFailedDialog(message)
            }
        }
    }


    // ==================== Scheduled Launch ====================

    fun onScheduledLaunch(request: ScheduledExecutionRequest) {
        coordinator.onLaunch(request)
    }

    fun onScheduledCountdownCancel() {
        coordinator.onCancel()
    }

    fun onScheduledStartNow() {
        coordinator.onStartNow()
    }

    fun onScheduledExecutionPageReady(requestId: String) {
        coordinator.onPageReady(requestId) { request ->
            _state.update {
                it.copy(
                    current = PanelTab.TASKS,
                    selectedNodeId = null,
                    isAddingTask = false,
                    isEditMode = false,
                    isProfileMode = false,
                )
            }
            startTasksInternal(
                request = request,
                context = TaskStartContext(mode = TaskStartMode.SCHEDULED),
            )?.resolve(application)
        }
    }

    // ==================== Surface ====================

    private fun onMonitorSurfaceChanged(
        service: RemoteService? = RemoteServiceManager.getInstanceOrNull()
    ) {
        val remote = service ?: return
        val surface = surfaceRef.get()
        Timber.d("onMonitorSurfaceChanged: surface=%s", surface)
        runCatching {
            remote.setMonitorSurface(surface)
        }.onFailure {
            Timber.w(it, "setMonitorSurface failed")
        }
    }

    fun onSurfaceAvailable(surface: Surface) {
        surfaceRef.set(surface)
        onMonitorSurfaceChanged()
    }

    fun onSurfaceDestroyed() {
        val surface = surfaceRef.getAndSet(null)
        onMonitorSurfaceChanged()
        surface?.release()
    }

    // ==================== Touch Input ====================

    fun onTouchDown(x: Int, y: Int) {
        runCatching {
            RemoteServiceManager.getInstanceOrNull()?.touchDown(x, y)
        }.onFailure {
            Timber.e(it, "touchDown failed at ($x, $y)")
        }
    }

    fun onTouchMove(x: Int, y: Int) {
        runCatching {
            RemoteServiceManager.getInstanceOrNull()?.touchMove(x, y)
        }.onFailure {
            Timber.e(it, "touchMove failed at ($x, $y)")
        }
    }

    fun onTouchUp(x: Int, y: Int) {
        runCatching {
            RemoteServiceManager.getInstanceOrNull()?.touchUp(x, y)
        }.onFailure {
            Timber.e(it, "touchUp failed at ($x, $y)")
        }
    }

    fun onScreenOff() {
        // 硬件熄屏：仅下发一次关闭物理屏幕的指令，无状态、幂等（再点必发，不会卡死）。
        // 启用该功能时 MainActivity 始终持有 FLAG_KEEP_SCREEN_ON 保持系统唤醒、不锁屏；
        // 屏幕恢复由系统在用户唤醒时处理，会话结束/服务销毁时由 PowerController 的 flag 兜底。
        val service = RemoteServiceManager.getInstanceOrNull()
        if (service == null) {
            Timber.w("onScreenOff skipped: remote service unavailable")
            return
        }
        runCatching { service.setDisplayPower(false) }
            .onFailure { Timber.e(it, "onScreenOff failed") }
    }

    // ==================== Task Chain ====================

    fun onNodeEnabledChange(nodeId: String, enabled: Boolean) {
        viewModelScope.launch {
            runCatching { chainState.setNodeEnabled(nodeId, enabled) }
                .onFailure { e ->
                    Timber.e(e, "Failed to update node enabled: ${e.message}")
                }
        }
    }

    fun onNodeMove(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            runCatching { chainState.reorderNodes(fromIndex, toIndex) }
                .onFailure { e ->
                    Timber.e(e, "Failed to reorder nodes: ${e.message}")
                }
        }
    }

    fun onNodeSelected(nodeId: String) {
        _state.update { it.copy(selectedNodeId = nodeId, isAddingTask = false) }
    }

    fun onToggleEditMode() {
        _state.update {
            it.copy(
                isEditMode = !it.isEditMode,
                isAddingTask = false,
                isProfileMode = false
            )
        }
        Timber.d("Edit mode toggled: %s", _state.value.isEditMode)
    }

    fun onToggleProfileMode() {
        _state.update {
            it.copy(
                isProfileMode = !it.isProfileMode,
                isEditMode = false,
                isAddingTask = false
            )
        }
        Timber.d("Profile mode toggled: %s", _state.value.isProfileMode)
    }

    fun onSwitchProfile(profileId: String) {
        viewModelScope.launch {
            chainState.switchProfile(profileId)
            _state.update { it.copy(selectedNodeId = null) }
        }
    }

    fun onCreateProfile() {
        viewModelScope.launch {
            chainState.createProfile()
            _state.update { it.copy(selectedNodeId = null) }
        }
    }

    fun onDeleteProfile(profileId: String) {
        viewModelScope.launch {
            chainState.deleteProfile(profileId)
            // PROFILE 定时解绑 + 可能因此变空的 SEQUENCE 定时一并消毒
            val detached = scheduleRepository.detachProfileConfig(profileId)
            val emptied = scheduleRepository.sanitizeInvalidTargets(
                profiles = chainState.profiles.value,
                sequenceConfigs = chainState.sequenceConfigs.value,
            )
            (detached + emptied).distinct().forEach { strategyId ->
                scheduleAlarmManager.cancel(strategyId)
            }
            _state.update { it.copy(selectedNodeId = null) }
        }
    }

    fun onRenameProfile(profileId: String, name: String) {
        viewModelScope.launch {
            chainState.renameProfile(profileId, name)
        }
    }

    fun onDuplicateProfile(profileId: String) {
        viewModelScope.launch {
            chainState.duplicateProfile(profileId)
        }
    }

    fun onReorderProfile(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            runCatching { chainState.reorderProfiles(fromIndex, toIndex) }
                .onFailure { e -> Timber.e(e, "Failed to reorder profile: ${e.message}") }
        }
    }

    fun onAddToSequence(profileId: String) {
        onAddProfilesToSequence(listOf(profileId))
    }

    fun onAddProfilesToSequence(profileIds: List<String>) {
        if (profileIds.isEmpty()) return
        viewModelScope.launch {
            chainState.addProfilesToSequence(profileIds)
        }
    }

    fun onRemoveSequenceEntry(entryId: String) {
        viewModelScope.launch {
            chainState.removeSequenceEntry(entryId)
        }
    }

    fun onReorderSequence(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            runCatching { chainState.reorderSequence(fromIndex, toIndex) }
                .onFailure { e -> Timber.e(e, "Failed to reorder sequence: ${e.message}") }
        }
    }

    fun onSetProfileSequenceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            chainState.setProfileSequenceEnabled(enabled)
        }
    }

    fun onSwitchSequenceConfig(configId: String) {
        viewModelScope.launch {
            chainState.switchSequenceConfig(configId)
        }
    }

    fun onCreateSequenceConfig() {
        viewModelScope.launch {
            chainState.createSequenceConfig()
        }
    }

    fun onRenameSequenceConfig(configId: String, name: String) {
        viewModelScope.launch {
            chainState.renameSequenceConfig(configId, name)
        }
    }

    fun onDeleteSequenceConfig(configId: String) {
        viewModelScope.launch {
            chainState.deleteSequenceConfig(configId)
            // 删除任务链后：禁用并解绑引用它的定时策略，同时取消已注册闹钟
            val detached = scheduleRepository.detachSequenceConfig(configId)
            detached.forEach { strategyId ->
                scheduleAlarmManager.cancel(strategyId)
            }
        }
    }

    fun onToggleAddingTask() {
        _state.update { it.copy(isAddingTask = !it.isAddingTask, selectedNodeId = null) }
        Timber.d("Adding task mode toggled: %s", _state.value.isAddingTask)
    }

    fun onAddNode(typeInfo: TaskTypeInfo) {
        viewModelScope.launch {
            val nodeId = chainState.addNode(typeInfo)
            _state.update { it.copy(isAddingTask = false, selectedNodeId = nodeId) }
        }
    }

    fun onRemoveNode(nodeId: String) {
        viewModelScope.launch {
            chainState.removeNode(nodeId)
            if (_state.value.selectedNodeId == nodeId) {
                _state.update { it.copy(selectedNodeId = null) }
            }
        }
    }

    fun onDuplicateNode(nodeId: String) {
        viewModelScope.launch {
            val newId = chainState.duplicateNode(nodeId)
            if (newId.isNotEmpty()) {
                _state.update { it.copy(selectedNodeId = newId) }
            }
        }
    }

    fun onRenameNode(nodeId: String, newName: String) {
        viewModelScope.launch {
            chainState.renameNode(nodeId, newName)
        }
    }

    fun onNodeConfigChange(nodeId: String, config: TaskParamProvider) {
        viewModelScope.launch {
            chainState.updateNodeConfig(nodeId, config)
        }
    }

    // ==================== UI State ====================

    fun onToggleFullscreenMonitor() {
        _state.update { it.copy(isFullscreenMonitor = !it.isFullscreenMonitor) }
    }

    fun onTabChange(tab: PanelTab) {
        _state.update { it.copy(current = tab) }
    }

    // ==================== Task Execution ====================

    fun onStartTasks() {
        launchManualStart(TaskStartContext(mode = TaskStartMode.MANUAL))
    }

    private fun launchManualStart(context: TaskStartContext) {
        viewModelScope.launch {
            val message = startTasksInternal(context = context)
            if (message != null && state.value.dialog == null) {
                showStartFailedDialog(message)
            }
        }
    }

    private suspend fun doSwitchProfile(request: ScheduledExecutionRequest?) {
        if (request == null || request.useSequence) return
        if (chainState.activeProfileId.value != request.profileId) {
            chainState.switchProfile(request.profileId)
        }
    }

    private suspend fun startTasksInternal(
        request: ScheduledExecutionRequest? = null,
        context: TaskStartContext,
    ): UiText? {
        doSwitchProfile(request)

        // 手动：按当前启用状态 resolve；定时任务链：强制按绑定配置拼接；定时 profile：单链
        val executableChain = when {
            request == null -> chainState.resolveExecutableChain()
            request.useSequence -> {
                val seq = chainState.sequenceConfigs.value
                    .find { it.id == request.sequenceConfigId }
                if (seq == null) emptyList()
                else chainState.resolveExecutableChain(
                    sequence = seq.entries,
                    sequenceEnabled = true,
                    fallbackToActive = false,
                )
            }
            else -> chainState.chain.value
        }

        val plan = when (
            val decision = prepareTaskStart(
                chain = executableChain,
                context = context,
            )
        ) {
            is TaskStartDecision.Ready -> {
                pendingStart = null
                // Queue trailing single-client segments (if any) for sequential execution.
                pendingClientPlans = decision.plans.drop(1)
                clientSegmentTotal = decision.plans.size
                if (decision.plans.size > 1) {
                    val clients = decision.plans.joinToString { it.clientType }
                    Timber.i(
                        "Multi-client chain split into %d segments: %s",
                        decision.plans.size,
                        clients,
                    )
                    _effects.send(
                        UiEffect.toast(
                            application.getString(
                                R.string.task_start_toast_multi_client_segments,
                                decision.plans.size,
                            ),
                        ),
                    )
                    if (decision.plans.groupingBy { it.clientType }.eachCount().any { it.value > 1 }) {
                        _effects.send(
                            UiEffect.toast(
                                application.getString(R.string.task_start_toast_client_revisit_hint),
                            ),
                        )
                    }
                }
                decision.plan
            }

            is TaskStartDecision.Blocked -> {
                pendingStart = null
                pendingClientPlans = emptyList()
                clientSegmentTotal = 0
                val message = application.resolveTaskStartDecisionMessage(decision)
                Timber.w("Validation failed: %s", message.resolve(application))
                if (request != null) {
                    showStartFailedDialog(message)
                } else {
                    showDialog(application.createStartBlockedDialog(message))
                }
                return message
            }

            is TaskStartDecision.RequiresConfirmation -> {
                pendingStart = PendingStart(context.acknowledged(decision.acknowledgement), request)
                val message = application.resolveTaskStartDecisionMessage(decision)
                showDialog(application.createStartWarningDialog(message))
                return message
            }
        }

        // 先静音后拉起游戏：appops 状态持久，提前设置零成本，消除游戏启动初期的漏音空窗
        val muteRequested = appSettingsManager.muteOnGameLaunch.value
        if (muteRequested && !gameMuteCoordinator.mute(plan.clientType)) {
            _effects.send(UiEffect.toast(R.string.bg_toast_mute_failed))
        }

        val allPlans = listOf(plan) + pendingClientPlans
        val result = compositionService.start(
            tasks = plan.params,
            clientType = plan.clientType,
            depotAccountTag = plan.depotAccountTag,
            isScheduled = context.mode == TaskStartMode.SCHEDULED,
            preflightLogs = plan.preflightLogs,
            expectDoubleSync = plan.unlockDoubleSync,
        ) {
            if (request != null) {
                sessionLogger.appendAndWait(
                    application.getString(
                        R.string.task_start_triggered_by_schedule,
                        request.strategyName,
                    ),
                )
            }
            if (allPlans.size > 1) {
                sessionLogger.appendAndWait(
                    application.getString(
                        R.string.runlog_client_segments_plan,
                        allPlans.size,
                        allPlans.joinToString { it.clientType },
                    ),
                )
                if (allPlans.groupingBy { it.clientType }.eachCount().any { it.value > 1 }) {
                    sessionLogger.appendAndWait(
                        application.getString(R.string.runlog_client_segments_revisit_hint),
                    )
                }
            }
            sessionLogger.appendAndWait(
                application.getString(
                    R.string.runlog_client_segment_start,
                    1,
                    allPlans.size,
                    plan.clientType,
                    plan.params.size,
                ),
            )
        }
        if (result is MaaCompositionService.StartResult.Success) {
            achievementReporter.reportTaskStarted(
                taskCount = plan.params.size,
                launchesGame = plan.launchesGame,
                gameAliveBeforeStart = plan.gameAliveBeforeStart,
            )
            chainState.grantGameBatteryExemption(plan.clientType)
        }

        val message = application.resolveTaskStartFailureMessage(result)
        if (message != null) {
            Timber.w("Start failed: %s", message.resolve(application))
            if (request != null) {
                showStartFailedDialog(message)
            }
            return message
        }
        return null
    }

    fun onStopTasks() {
        achievementReporter.reportTaskStopped()
        pendingClientPlans = emptyList()
        clientSegmentTotal = 0
        viewModelScope.launch {
            compositionService.stop()
        }
    }

    fun onClearLogs() {
        sessionLogger.clearRuntimeLogs()
    }

    fun onToggleGameSound() {
        viewModelScope.launch {
            val ok = gameMuteCoordinator.toggle(chainState.getClientTypeOrNull())
            if (!ok) {
                _effects.send(UiEffect.toast(R.string.bg_toast_mute_failed))
            }
        }
    }

    /**
     * 调试用：请求远端进程抓取当前帧缓冲并保存 PNG 到 {rootDir}/debug/screenshots，
     * 结果通过 [screenshotMessage] 反馈给 UI。
     *
     * 由远端（shell 进程）直接落盘——它对 userDir/debug 有写权限（同 logcat 抓取），
     * 避免跨进程读取 ashmem 被 SELinux 拒绝。
     */
    fun onCaptureDebugScreenshot() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedName = runCatching {
                RemoteServiceManager.getInstanceOrNull()
                    ?.captureFramePng(pathConfig.debugScreenshotsDir)
                    ?.let { File(it).name }
            }.onFailure { Timber.e(it, "captureDebugScreenshot failed") }
                .getOrNull()
            val message = savedName
                ?.let { application.getString(R.string.bg_toast_screenshot_saved, it) }
                ?: application.getString(R.string.bg_toast_screenshot_failed)
            _screenshotMessage.tryEmit(message)
        }
    }

    private fun showStartFailedDialog(message: UiText) {
        showDialog(application.createStartFailedDialog(message))
    }

    // ==================== Dialog ====================

    private fun showDialog(dialog: PanelDialogUiState) {
        _state.update { it.copy(dialog = dialog) }
    }

    fun onDialogDismiss() {
        pendingStart = null
        // Keep pendingClientPlans only if a multi-segment run is already in flight;
        // dismiss before start should not leave a stale queue.
        if (compositionService.state.value == MaaExecutionState.IDLE ||
            compositionService.state.value == MaaExecutionState.ERROR
        ) {
            pendingClientPlans = emptyList()
        }
        _state.update { it.copy(dialog = null) }
    }

    fun onDialogConfirm() {
        when (state.value.dialog?.confirmAction) {
            PanelDialogConfirmAction.DISMISS_ONLY -> {
                onDialogDismiss()
            }

            PanelDialogConfirmAction.CONFIRM_PENDING_START -> {
                val pending = pendingStart
                _state.update { it.copy(dialog = null) }
                pendingStart = null
                if (pending != null) {
                    viewModelScope.launch {
                        val message = startTasksInternal(
                            request = pending.request,
                            context = pending.context,
                        )
                        if (message != null && state.value.dialog == null) {
                            showStartFailedDialog(message)
                        }
                    }
                }
            }

            PanelDialogConfirmAction.GO_LOG -> {
                onTabChange(PanelTab.LOG)
                onDialogDismiss()
            }

            PanelDialogConfirmAction.GO_LOG_AND_STOP -> {
                onTabChange(PanelTab.LOG)
                onDialogDismiss()
                viewModelScope.launch {
                    compositionService.stop()
                }
            }

            null -> Unit
        }
    }

    override fun onCleared() {
        coordinator.cancel()
        touchPreviewController.onClear()
        super.onCleared()
    }
}
