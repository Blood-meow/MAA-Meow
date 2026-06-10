package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.domain.service.ResourceInitService
import com.aliothmoon.maameow.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsViewMiuix(
    navController: NavController,
    onViewAnnouncement: () -> Unit,
    viewModel: SettingsViewModel,
    resourceInitService: ResourceInitService,
    logExportService: LogExportService
) {
    SettingsViewMaterial(navController, onViewAnnouncement, viewModel, resourceInitService, logExportService)
}
