package com.aliothmoon.maameow.presentation.view.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.presentation.viewmodel.HomeViewModel
import com.aliothmoon.maameow.presentation.viewmodel.UpdateViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HomeView(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
    permissionManager: PermissionManager = koinInject(),
    appSettingsManager: AppSettingsManager = koinInject()
) {
    if (isMiuixUi) {
        HomeViewMiuix(
            navController = navController,
            viewModel = viewModel,
            updateViewModel = updateViewModel,
            permissionManager = permissionManager,
            appSettingsManager = appSettingsManager
        )
    } else {
        HomeViewMaterial(
            navController = navController,
            viewModel = viewModel,
            updateViewModel = updateViewModel,
            permissionManager = permissionManager,
            appSettingsManager = appSettingsManager
        )
    }
}
