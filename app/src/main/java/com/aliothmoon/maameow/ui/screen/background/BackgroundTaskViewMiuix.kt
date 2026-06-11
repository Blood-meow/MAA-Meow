package com.aliothmoon.maameow.ui.screen.background

import androidx.compose.runtime.Composable
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.domain.service.UnifiedStateDispatcher
import com.aliothmoon.maameow.domain.service.AppWatchdog
import com.aliothmoon.maameow.overlay.screensaver.ScreenSaverOverlayManager
import com.aliothmoon.maameow.ui.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.ui.viewmodel.CopilotViewModel
import com.aliothmoon.maameow.ui.viewmodel.ToolboxViewModel

@Composable
fun BackgroundTaskViewMiuix(
    onFullscreenChanged: (Boolean) -> Unit,
    viewModel: BackgroundTaskViewModel,
    copilotViewModel: CopilotViewModel,
    toolboxViewModel: ToolboxViewModel,
    compositionService: MaaCompositionService,
    dispatcher: UnifiedStateDispatcher,
    permissionManager: PermissionManager,
    screenSaverManager: ScreenSaverOverlayManager,
    appWatchdog: AppWatchdog
) {
    BackgroundTaskViewMaterial(onFullscreenChanged, viewModel, copilotViewModel, toolboxViewModel, compositionService, dispatcher, permissionManager, screenSaverManager, appWatchdog)
}
