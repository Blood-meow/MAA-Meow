package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.ui.viewmodel.LogHistoryViewModel

@Composable
fun LogHistoryViewMiuix(
    navController: NavController, viewModel: LogHistoryViewModel, logExportService: LogExportService
) {
    LogHistoryViewMaterial(navController, viewModel, logExportService)
}
