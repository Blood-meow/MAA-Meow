package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.viewmodel.SettingsViewModel

@Composable
fun ThemeSettingsViewMiuix(
    navController: NavController, viewModel: SettingsViewModel
) {
    ThemeSettingsViewMaterial(navController, viewModel)
}
