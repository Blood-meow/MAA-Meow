package com.aliothmoon.maameow.ui.state

import com.aliothmoon.maameow.ui.screen.panel.PanelDialogUiState
import com.aliothmoon.maameow.ui.screen.panel.PanelTab

data class BackgroundTaskState(
    val selectedNodeId: String? = null,
    val current: PanelTab = PanelTab.TASKS,
    val isFullscreenMonitor: Boolean = false,
    val isEditMode: Boolean = false,
    val isAddingTask: Boolean = false,
    val isProfileMode: Boolean = false,
    val dialog: PanelDialogUiState? = null,
)
