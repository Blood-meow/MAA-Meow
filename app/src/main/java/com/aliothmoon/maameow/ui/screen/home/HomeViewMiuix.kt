package com.aliothmoon.maameow.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeViewMiuix(
    navController: NavController,
    viewModel: HomeViewModel,
    updateViewModel: UpdateViewModel,
    permissionManager: PermissionManager,
    appSettingsManager: AppSettingsManager
) {
    MiuixSurface(
        modifier = Modifier.fillMaxSize(),
        color = MiuixTheme.colorScheme.background
    ) {
        HomeViewMaterial(navController, viewModel, updateViewModel, permissionManager, appSettingsManager)
    }
}
