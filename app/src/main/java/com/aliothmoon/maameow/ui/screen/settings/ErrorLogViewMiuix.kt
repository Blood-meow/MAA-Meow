package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.viewmodel.ErrorLogViewModel


@Composable
fun ErrorLogViewMiuix(
    navController: NavController, viewModel: ErrorLogViewModel
) {
    ErrorLogViewMaterial(navController, viewModel)
}
