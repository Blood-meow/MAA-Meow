package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.domain.service.ResourceInitService
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsViewMiuix(
    navController: NavController,
    onViewAnnouncement: () -> Unit,
    viewModel: SettingsViewModel,
    resourceInitService: ResourceInitService,
    logExportService: LogExportService
) {
    // Complex settings page with many Material inner components (RadioButton, Switch, InfoCard, etc.).
    // Material version already uses ThemeColors for Miuix color compatibility.
    SettingsViewMaterial(navController, onViewAnnouncement, viewModel, resourceInitService, logExportService)
}
