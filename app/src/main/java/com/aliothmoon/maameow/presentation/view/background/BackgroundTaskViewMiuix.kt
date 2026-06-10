package com.aliothmoon.maameow.presentation.view.background

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.domain.service.UnifiedStateDispatcher
import com.aliothmoon.maameow.domain.service.AppWatchdog
import com.aliothmoon.maameow.overlay.screensaver.ScreenSaverOverlayManager
import com.aliothmoon.maameow.presentation.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.presentation.viewmodel.CopilotViewModel
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel

@Composable
fun BackgroundTaskViewMiuix(
    onFullscreenChanged: (Boolean) -> Unit, viewModel: BackgroundTaskViewModel, copilotViewModel: CopilotViewModel, toolboxViewModel: ToolboxViewModel, compositionService: MaaCompositionService, dispatcher: UnifiedStateDispatcher, permissionManager: PermissionManager, screenSaverManager: ScreenSaverOverlayManager, appWatchdog: AppWatchdog
) {
    BackgroundTaskViewMaterial(onFullscreenChanged, viewModel, copilotViewModel, toolboxViewModel, compositionService, dispatcher, permissionManager, screenSaverManager, appWatchdog)
}
