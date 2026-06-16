package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.domain.service.ResourceInitService
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsView(
    navController: NavController,
    onViewAnnouncement: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
    resourceInitService: ResourceInitService = koinInject(),
    logExportService: LogExportService = koinInject()
) {
    if (isMiuixUi) {
        SettingsViewMiuix(navController, onViewAnnouncement, viewModel, resourceInitService, logExportService)
    } else {
        SettingsViewMaterial(navController, onViewAnnouncement, viewModel, resourceInitService, logExportService)
    }
}
