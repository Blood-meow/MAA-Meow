package com.aliothmoon.maameow.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel

@Composable
fun HomeViewMiuix(
    navController: NavController,
    viewModel: HomeViewModel,
    updateViewModel: UpdateViewModel,
    permissionManager: PermissionManager,
    appSettingsManager: AppSettingsManager
) {
    HomeViewMaterial(navController, viewModel, updateViewModel, permissionManager, appSettingsManager)
}
