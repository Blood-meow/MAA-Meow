package com.aliothmoon.maameow.presentation.view.notification

import androidx.compose.runtime.Composable
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.presentation.view.notification.NotificationSettingsViewMaterial
import com.aliothmoon.maameow.presentation.view.notification.NotificationSettingsViewMiuix
import com.aliothmoon.maameow.presentation.view.notification.NotificationSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationSettingsViewMaterial(
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    if (isMiuixUi) {
        NotificationSettingsViewMiuix(viewModel)
    } else {
        NotificationSettingsViewMaterial(viewModel)
    }
}
