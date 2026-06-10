package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.screen.settings.ThemeSettingsViewMaterial
import com.aliothmoon.maameow.ui.screen.settings.ThemeSettingsViewMiuix
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeSettingsView(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    if (isMiuixUi) {
        ThemeSettingsViewMiuix(navController, viewModel)
    } else {
        ThemeSettingsViewMaterial(navController, viewModel)
    }
}
