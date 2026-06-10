package com.aliothmoon.maameow.presentation.view.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.service.PermissionManager
import com.aliothmoon.maameow.presentation.viewmodel.HomeViewModel
import com.aliothmoon.maameow.presentation.viewmodel.UpdateViewModel

@Composable
fun HomeViewMiuix(
    navController: NavController,
    viewModel: HomeViewModel,
    updateViewModel: UpdateViewModel,
    permissionManager: PermissionManager,
    appSettingsManager: AppSettingsManager
) {
    HomeViewMaterial(
        navController = navController,
        viewModel = viewModel,
        updateViewModel = updateViewModel,
        permissionManager = permissionManager,
        appSettingsManager = appSettingsManager
    )
}
