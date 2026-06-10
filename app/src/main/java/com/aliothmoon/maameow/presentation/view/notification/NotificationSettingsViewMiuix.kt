package com.aliothmoon.maameow.presentation.view.notification

import androidx.compose.runtime.Composable
import com.aliothmoon.maameow.presentation.view.notification.NotificationSettingsViewMaterial
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationSettingsViewMiuix(
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    NotificationSettingsViewMaterial(viewModel)
}
