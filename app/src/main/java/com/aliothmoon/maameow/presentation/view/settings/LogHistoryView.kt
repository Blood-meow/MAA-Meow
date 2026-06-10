package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.presentation.view.settings.LogHistoryViewMaterial
import com.aliothmoon.maameow.presentation.view.settings.LogHistoryViewMiuix
import com.aliothmoon.maameow.presentation.viewmodel.LogHistoryViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun LogHistoryView(
    navController: NavController,
    viewModel: LogHistoryViewModel = koinViewModel(),
    logExportService: LogExportService = koinInject()
) {
    if (isMiuixUi) {
        LogHistoryViewMiuix(navController, viewModel, logExportService)
    } else {
        LogHistoryViewMaterial(navController, viewModel, logExportService)
    }
}
