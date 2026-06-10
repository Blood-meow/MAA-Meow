package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.presentation.view.settings.ErrorLogViewMaterial
import com.aliothmoon.maameow.presentation.view.settings.ErrorLogViewMiuix
import com.aliothmoon.maameow.presentation.viewmodel.ErrorLogViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ErrorLogView(
    navController: NavController,
    viewModel: ErrorLogViewModel = koinViewModel()
) {
    if (isMiuixUi) {
        ErrorLogViewMiuix(navController, viewModel)
    } else {
        ErrorLogViewMaterial(navController, viewModel)
    }
}
