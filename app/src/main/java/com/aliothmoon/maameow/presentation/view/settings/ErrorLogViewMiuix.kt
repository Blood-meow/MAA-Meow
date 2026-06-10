package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.view.settings.ErrorLogViewMaterial
import org.koin.androidx.compose.koinViewModel

@Composable
fun ErrorLogViewMiuix(
    navController: NavController,
    viewModel: ErrorLogViewModel = koinViewModel()
) {
    ErrorLogViewMaterial(navController, viewModel)
}
