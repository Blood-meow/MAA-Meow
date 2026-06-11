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
    // MaaUiScaffold + MaaUiTopAppBar auto-switch to MiuixScaffold + MiuixTopAppBar.
    // InfoCard/MaaUiCard use Miuix-aware colors (ThemeColors.surfaceContainer, alpha 0.72).
    // Inner Material components (Switch, RadioButton) render correctly under MiuixTheme.
    SettingsViewMaterial(navController, onViewAnnouncement, viewModel, resourceInitService, logExportService)
}
