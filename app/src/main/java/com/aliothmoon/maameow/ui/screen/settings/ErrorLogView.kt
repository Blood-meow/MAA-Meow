package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.screen.settings.ErrorLogViewMaterial
import com.aliothmoon.maameow.ui.screen.settings.ErrorLogViewMiuix
import com.aliothmoon.maameow.ui.viewmodel.ErrorLogViewModel
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
