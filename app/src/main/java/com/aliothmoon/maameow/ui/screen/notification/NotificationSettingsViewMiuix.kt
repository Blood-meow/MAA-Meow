package com.aliothmoon.maameow.ui.screen.notification

import androidx.compose.runtime.Composable
import com.aliothmoon.maameow.ui.viewmodel.NotificationSettingsViewModel
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
fun NotificationSettingsViewMiuix(
    viewModel: NotificationSettingsViewModel
) {
    // Delegate to Material implementation which already uses ThemeColors for Miuix compat.
    // The inner content (ITextField, RadioButton, Switch, InfoCard) has no native Miuix equivalents.
    NotificationSettingsViewMaterial(viewModel)
}
