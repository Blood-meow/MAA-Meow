package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.viewmodel.AchievementViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AchievementView(
    navController: NavController,
    viewModel: AchievementViewModel = koinViewModel(),
) {
    if (isMiuixUi) {
        AchievementViewMiuix(navController = navController, viewModel = viewModel)
    } else {
        AchievementViewMaterial(navController = navController, viewModel = viewModel)
    }
}
