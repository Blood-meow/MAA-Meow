package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.presentation.view.settings.ThemeSettingsViewMaterial
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeSettingsViewMiuix(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    ThemeSettingsViewMaterial(navController, viewModel)
}
