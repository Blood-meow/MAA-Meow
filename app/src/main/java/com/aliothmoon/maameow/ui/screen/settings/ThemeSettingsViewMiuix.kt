package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel

@Composable
fun ThemeSettingsViewMiuix(
    navController: NavController, viewModel: SettingsViewModel
) {
    ThemeSettingsViewMaterial(navController, viewModel)
}
