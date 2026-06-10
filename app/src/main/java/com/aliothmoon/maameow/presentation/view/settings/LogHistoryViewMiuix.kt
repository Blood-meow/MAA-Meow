package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.presentation.view.settings.LogHistoryViewMaterial
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun LogHistoryViewMiuix(
    navController: NavController,
    viewModel: LogHistoryViewModel = koinViewModel(),
    logExportService: LogExportService = koinInject()
) {
    LogHistoryViewMaterial(navController, viewModel, logExportService)
}
