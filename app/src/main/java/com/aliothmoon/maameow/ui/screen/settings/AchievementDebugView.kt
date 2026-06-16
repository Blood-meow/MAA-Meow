package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.ui.isMiuixUi
import org.koin.compose.koinInject

@Composable
fun AchievementDebugView(
    navController: NavController,
    repository: AchievementRepository = koinInject(),
) {
    if (isMiuixUi) {
        AchievementDebugViewMiuix(navController = navController, repository = repository)
    } else {
        AchievementDebugViewMaterial(navController = navController, repository = repository)
    }
}
