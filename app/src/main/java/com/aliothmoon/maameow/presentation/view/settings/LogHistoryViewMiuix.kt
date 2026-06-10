package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.LogExportService

@Composable
fun LogHistoryViewMiuix(
    navController: NavController, viewModel: LogHistoryViewModel, logExportService: LogExportService
) {
    LogHistoryViewMaterial(navController, viewModel, logExportService)
}
