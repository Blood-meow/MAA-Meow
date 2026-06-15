package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.screen.settings.ThemeSettingsViewMaterial
import com.aliothmoon.maameow.ui.screen.settings.ThemeSettingsViewMiuix
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ThemeSettingsView(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
    homeViewModel: HomeViewModel = koinViewModel(),
    permissionManager: PermissionManager = koinInject(),
) {
    if (isMiuixUi) {
        ThemeSettingsViewMiuix(navController, viewModel, updateViewModel, homeViewModel, permissionManager)
    } else {
        ThemeSettingsViewMaterial(navController, viewModel, updateViewModel, homeViewModel, permissionManager)
    }
}
