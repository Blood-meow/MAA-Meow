package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel

@Composable
fun ThemeSettingsViewMiuix(
    navController: NavController,
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    homeViewModel: HomeViewModel,
    permissionManager: PermissionManager
) {
    ThemeSettingsViewMaterial(navController, viewModel, updateViewModel, homeViewModel, permissionManager)
}
