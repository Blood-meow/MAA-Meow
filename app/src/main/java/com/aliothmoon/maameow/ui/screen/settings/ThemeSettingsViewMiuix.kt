package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ThemeSettingsViewMiuix(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
    homeViewModel: HomeViewModel = koinViewModel(),
    permissionManager: PermissionManager = koinInject()
) {
    // Use MiuixScaffold for proper miuix chrome, but delegate inner content to Material version
    // which already uses ThemeColors for Miuix color compatibility.
    ThemeSettingsViewMaterial(navController, viewModel, updateViewModel, homeViewModel, permissionManager)
}
