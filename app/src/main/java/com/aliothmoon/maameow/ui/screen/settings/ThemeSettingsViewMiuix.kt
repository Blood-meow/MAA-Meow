package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.asString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

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
