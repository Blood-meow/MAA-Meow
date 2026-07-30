package com.aliothmoon.maameow.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.data.model.LogItem
import com.aliothmoon.maameow.data.model.TaskParamProvider
import com.aliothmoon.maameow.data.model.TaskTypeInfo
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.domain.service.GameMuteCoordinator
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.service.MaaSessionLogger
import com.aliothmoon.maameow.domain.service.AchievementReporter
import com.aliothmoon.maameow.domain.usecase.PrepareTaskStartUseCase
import com.aliothmoon.maameow.domain.usecase.TaskChainPlan
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.domain.usecase.TaskStartContext
import com.aliothmoon.maameow.domain.usecase.TaskStartDecision
import com.aliothmoon.maameow.domain.usecase.TaskStartMode
import com.aliothmoon.maameow.overlay.OverlayController
import com.aliothmoon.maameow.presentation.state.UiEffect
import com.aliothmoon.maameow.presentation.view.panel.FloatingPanelState
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogConfirmAction
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogUiState
import com.aliothmoon.maameow.presentation.view.panel.PanelTab
import com.aliothmoon.maameow.schedule.data.ScheduleStrategyRepository
import com.aliothmoon.maameow.schedule.service.ScheduleAlarmManager
import com.aliothmoon.maameow.utils.i18n.resolve
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber


class ExpandedControlPanelViewModel(
    val chainState: TaskChainState,
    private val application: Context,
    private val prepareTaskStart: PrepareTaskStartUseCase,
    private val compositionService: MaaCompositionService,
    private val overlayController: OverlayController,
    private val sessionLogger: MaaSessionLogger,
    private val achievementReporter: AchievementReporter,
    private val scheduleRepository: ScheduleStrategyRepository,
    private val scheduleAlarmManager: ScheduleAlarmManager,
    private val appSettingsManager: AppSettingsManager,
    private val gameMuteCoordinator: GameMuteCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(FloatingPanelState())
    val state: StateFlow<FloatingPanelState> = _state.asStateFlow()
    val runtimeLogs: StateFlow<List<LogItem>> = sessionLogger.logs

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var pendingStartContext: TaskStartContext? = null

    private var pendingClientPlans: List<TaskChainPlan> = emptyList()
    private var clientSegmentTotal: Int = 0

    init {
        observeTaskEndForClientQueue()
        viewModelScope.launch {
            overlayController.signal.collect { endState ->
                Timber.d("Overlay result received: $endState")
                showDialog(application.createExecutionEndDialog(endState))
            }
        }
        observeDefaultTaskSelection()
    }

    /**
     * 首次展开 / 选中失效时默认打开任务链第一项，避免右侧一直停在空占位
     * 新增任务、配置管理模式下不自动改写选中
     */
    private fun observeDefaultTaskSelection() {
        viewModelScope.launch {
            combine(chainState.chain, _state) { nodes, ui ->
                resolveTaskPanelSelectedNodeId(
                    nodes = nodes,
                    selectedNodeId = ui.selectedNodeId,
                    isAddingTask = ui.isAddingTask,
                    isProfileMode = ui.isProfileMode,
                )
            }
                .distinctUntilChanged()
                .collect { resolved ->
                    if (_state.value.selectedNodeId != resolved) {
                        _state.update { it.copy(selectedNodeId = resolved) }
                    }
                }
        }
    }

    fun onNodeEnabledChange(nodeId: String, enabled: Boolean) {
        viewModelScope.launch {
            runCatching { chainState.setNodeEnabled(nodeId, enabled) }
                .onSuccess {
                    Timber.d("Updated node %s enabled: %s", nodeId, enabled)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to update node enabled: ${e.message}")
                }
        }
    }

    fun onNodeMove(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            runCatching { chainState.reorderNodes(fromIndex, toIndex) }
                .onSuccess {
                    Timber.d("Moved node from %d to %d", fromIndex, toIndex)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to reorder nodes: ${e.message}")
                }
        }
    }

    fun onNodeSelected(nodeId: String) {
        _state.update { it.copy(selectedNodeId = nodeId, isAddingTask = false) }
        Timber.d("Selected node: %s", nodeId)
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
            // 切换后清除选中状态
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

    fun onTabChange(tab: PanelTab) {
        _state.update { it.copy(currentTab = tab) }
        Timber.d("Selected tab: %s", tab.name)
    }

    private fun showDialog(dialog: PanelDialogUiState) {
        _state.update { it.copy(dialog = dialog) }
    }

    fun onDialogDismiss() {
        pendingStartContext = null
        _state.update { it.copy(dialog = null) }
    }

    fun onDialogConfirm() {
        when (state.value.dialog?.confirmAction) {
            PanelDialogConfirmAction.DISMISS_ONLY -> {
                onDialogDismiss()
            }

            PanelDialogConfirmAction.CONFIRM_PENDING_START -> {
                val pending = pendingStartContext
                _state.update { it.copy(dialog = null) }
                pendingStartContext = null
                if (pending != null) {
                    launchManualStart(pending)
                }
            }

            PanelDialogConfirmAction.GO_LOG -> {
                onTabChange(PanelTab.LOG)
                onDialogDismiss()
            }

            PanelDialogConfirmAction.GO_LOG_AND_STOP -> {
                onTabChange(PanelTab.LOG)
                onDialogDismiss()
                onStopTasks()
            }

            null -> Unit
        }
    }

    fun onClearLogs() {
        sessionLogger.clearRuntimeLogs()
    }

    /**
     * User-initiated stop: drop any queued multi-client segments before stop(),
     * so a RUNNING→IDLE transition (or conflated StateFlow skip of STOPPING) cannot
     * auto-start the next client segment.
     */
    fun onStopTasks() {
        clearPendingClientSegments()
        viewModelScope.launch {
            compositionService.stop()
        }
    }

    private fun clearPendingClientSegments() {
        pendingClientPlans = emptyList()
        clientSegmentTotal = 0
    }

    fun onStartTasks() {
        launchManualStart(TaskStartContext(mode = TaskStartMode.MANUAL))
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
            val detached = scheduleRepository.detachSequenceConfig(configId)
            detached.forEach { strategyId ->
                scheduleAlarmManager.cancel(strategyId)
            }
        }
    }

    private fun launchManualStart(context: TaskStartContext) {
        viewModelScope.launch {
            val plan = when (
                val decision = prepareTaskStart(
                    chain = chainState.resolveExecutableChain(),
                    context = context,
                )
            ) {
                is TaskStartDecision.Ready -> {
                    pendingStartContext = null
                    pendingClientPlans = decision.plans.drop(1)
                    clientSegmentTotal = decision.plans.size
                    if (decision.plans.size > 1) {
                        Timber.i(
                            "Multi-client chain split into %d segments: %s",
                            decision.plans.size,
                            decision.plans.joinToString { it.clientType },
                        )
                        _effects.send(
                            UiEffect.toast(
                                application.getString(
                                    R.string.task_start_toast_multi_client_segments,
                                    decision.plans.size,
                                ),
                            ),
                        )
                    }
                    decision.plan
                }

                is TaskStartDecision.Blocked -> {
                    pendingStartContext = null
                    pendingClientPlans = emptyList()
                    clientSegmentTotal = 0
                    val message = application.resolveTaskStartDecisionMessage(decision)
                    Timber.w("Validation failed: %s", message.resolve(application))
                    showDialog(application.createStartBlockedDialog(message))
                    return@launch
                }

                is TaskStartDecision.RequiresConfirmation -> {
                    pendingStartContext = context.acknowledged(decision.acknowledgement)
                    showDialog(
                        application.createStartWarningDialog(
                            application.resolveTaskStartDecisionMessage(decision)
                        )
                    )
                    return@launch
                }
            }

            Timber.i("=== Task JSON List (%d tasks) ===", plan.params.size)
            plan.params.forEachIndexed { index, params ->
                Timber.i("[%d] Type: %s", index, params.type.value)
                Timber.i("    Params: %s", params.params)
            }
            Timber.i("=== End Task JSON List ===")

            val muteRequested = appSettingsManager.muteOnGameLaunch.value
            if (muteRequested && !gameMuteCoordinator.mute(plan.clientType)) {
                _effects.send(
                    UiEffect.toast(R.string.bg_toast_mute_failed),
                )
            }

            val allPlans = listOf(plan) + pendingClientPlans
            val result = compositionService.start(
                tasks = plan.params,
                clientType = plan.clientType,
                depotAccountTag = plan.depotAccountTag,
                preflightLogs = plan.preflightLogs,
            ) {
                if (allPlans.size > 1) {
                    sessionLogger.appendAndWait(
                        application.getString(
                            R.string.runlog_client_segments_plan,
                            allPlans.size,
                            allPlans.joinToString { it.clientType },
                        ),
                    )
                }
                sessionLogger.appendAndWait(
                    application.getString(
                        R.string.runlog_client_segment_start,
                        1,
                        allPlans.size.coerceAtLeast(1),
                        plan.clientType,
                        plan.params.size,
                    ),
                )
            }
            val message = application.formatStartResult(result)
            if (result is MaaCompositionService.StartResult.Success) {
                achievementReporter.reportTaskStarted(
                    taskCount = plan.params.size,
                    launchesGame = plan.launchesGame,
                    gameAliveBeforeStart = plan.gameAliveBeforeStart,
                )
                // 成功时用 Toast 简短提示
                _effects.send(UiEffect.toast(message))
            } else {
                // 失败时通过 StateFlow 通知 UI 展示 OverlayDialog
                Timber.w("Start failed: %s", message.resolve(application))
                showDialog(application.createStartFailedDialog(message))
            }
        }
    }

    private fun observeTaskEndForClientQueue() {
        viewModelScope.launch {
            var prev = compositionService.state.value
            compositionService.state.collect { current ->
                // Any stop path enters STOPPING first; clear queue so a later IDLE
                // cannot be mistaken for a natural segment end. Also covers float-ball /
                // volume-key stops that call compositionService.stop() outside this VM.
                if (current == MaaExecutionState.STOPPING) {
                    clearPendingClientSegments()
                }
                // Manual stop: RUNNING → STOPPING → IDLE (prev is STOPPING, no match).
                // StateFlow may conflate and skip STOPPING; clear-on-STOPPING + onStopTasks
                // still protect that race.
                val naturalIdle = prev == MaaExecutionState.RUNNING && current == MaaExecutionState.IDLE
                val naturalError = prev == MaaExecutionState.RUNNING && current == MaaExecutionState.ERROR
                if (naturalIdle && pendingClientPlans.isNotEmpty()) {
                    val remaining = pendingClientPlans
                    pendingClientPlans = emptyList()
                    continueClientSegmentQueue(remaining)
                } else if (naturalError && pendingClientPlans.isNotEmpty()) {
                    Timber.w("Overlay segment ERROR; drop %d remaining", pendingClientPlans.size)
                    clearPendingClientSegments()
                    viewModelScope.launch {
                        sessionLogger.appendAndWait(
                            application.getString(
                                R.string.runlog_client_segment_aborted,
                                current.name,
                            ),
                        )
                    }
                }
                prev = current
            }
        }
    }

    private suspend fun continueClientSegmentQueue(remaining: List<TaskChainPlan>) {
        val next = remaining.firstOrNull() ?: return
        val rest = remaining.drop(1)
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
                application.getString(R.string.task_start_toast_next_client_segment, next.clientType),
            ),
        )
        runCatching { compositionService.stopVirtualDisplay() }
        val muteRequested = appSettingsManager.muteOnGameLaunch.value
        if (muteRequested && !gameMuteCoordinator.mute(next.clientType)) {
            _effects.send(
                UiEffect.toast(R.string.bg_toast_mute_failed),
            )
        }
        val result = compositionService.start(
            tasks = next.params,
            clientType = next.clientType,
            depotAccountTag = next.depotAccountTag,
            preflightLogs = next.preflightLogs,
        ) {
            sessionLogger.appendAndWait(
                application.getString(
                    R.string.runlog_client_segment_start,
                    segmentIndex,
                    clientSegmentTotal.coerceAtLeast(1),
                    next.clientType,
                    next.params.size,
                ),
            )
        }
        if (result is MaaCompositionService.StartResult.Success) {
            pendingClientPlans = rest
            achievementReporter.reportTaskStarted(
                taskCount = next.params.size,
                launchesGame = next.launchesGame,
                gameAliveBeforeStart = next.gameAliveBeforeStart,
            )
            if (rest.isEmpty()) {
                sessionLogger.appendAndWait(application.getString(R.string.runlog_client_segment_done_all))
                clientSegmentTotal = 0
            }
        } else {
            pendingClientPlans = emptyList()
            clientSegmentTotal = 0
            val message = application.formatStartResult(result)
            sessionLogger.appendAndWait(
                application.getString(
                    R.string.runlog_client_segment_aborted,
                    message.resolve(application),
                ),
            )
            showDialog(application.createStartFailedDialog(message))
        }
    }


}
