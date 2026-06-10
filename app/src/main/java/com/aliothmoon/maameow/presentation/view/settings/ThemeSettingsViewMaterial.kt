package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.presentation.view.settings.SettingsViewModel
import com.aliothmoon.maameow.presentation.view.settings.ThemeSettingsViewMaterial
import com.aliothmoon.maameow.presentation.view.settings.ThemeSettingsViewMiuix
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeSettingsViewMaterial(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    if (isMiuixUi) {
        ThemeSettingsViewMiuix(navController, viewModel)
    } else {
        ThemeSettingsViewMaterial(navController, viewModel)
    }
}
