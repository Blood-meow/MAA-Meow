package com.aliothmoon.maameow.presentation.view.background

import androidx.compose.runtime.Composable
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.domain.service.UnifiedStateDispatcher
import com.aliothmoon.maameow.domain.service.AppWatchdog
import com.aliothmoon.maameow.overlay.screensaver.ScreenSaverOverlayManager
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.presentation.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.presentation.viewmodel.CopilotViewModel
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel
import org.koin.compose.koinInject

@Composable
fun BackgroundTaskView(
    onFullscreenChanged: (Boolean) -> Unit = {},
    viewModel: BackgroundTaskViewModel,
    copilotViewModel: CopilotViewModel = koinInject(),
    toolboxViewModel: ToolboxViewModel = koinInject(),
    compositionService: MaaCompositionService = koinInject(),
    dispatcher: UnifiedStateDispatcher = koinInject(),
    permissionManager: PermissionManager = koinInject(),
    screenSaverManager: ScreenSaverOverlayManager = koinInject(),
    appWatchdog: AppWatchdog = koinInject(),
) {
    if (isMiuixUi) {
        BackgroundTaskViewMiuix(onFullscreenChanged, viewModel, copilotViewModel, toolboxViewModel, compositionService, dispatcher, permissionManager, screenSaverManager, appWatchdog)
    } else {
        BackgroundTaskViewMaterial(onFullscreenChanged, viewModel, copilotViewModel, toolboxViewModel, compositionService, dispatcher, permissionManager, screenSaverManager, appWatchdog)
    }
}
