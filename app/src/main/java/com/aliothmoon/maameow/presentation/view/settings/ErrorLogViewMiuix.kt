package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.viewmodel.ErrorLogViewModel


@Composable
fun ErrorLogViewMiuix(
    navController: NavController, viewModel: ErrorLogViewModel
) {
    ErrorLogViewMaterial(navController, viewModel)
}
