package com.aliothmoon.maameow.ui.screen.notification

import androidx.compose.runtime.Composable
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.screen.notification.NotificationSettingsViewMaterial
import com.aliothmoon.maameow.ui.screen.notification.NotificationSettingsViewMiuix
import com.aliothmoon.maameow.ui.viewmodel.NotificationSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationSettingsView(
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    if (isMiuixUi) {
        NotificationSettingsViewMiuix(viewModel)
    } else {
        NotificationSettingsViewMaterial(viewModel)
    }
}
