package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController


@Composable
fun ErrorLogViewMiuix(
    navController: NavController, viewModel: ErrorLogViewModel
) {
    ErrorLogViewMaterial(navController, viewModel)
}
