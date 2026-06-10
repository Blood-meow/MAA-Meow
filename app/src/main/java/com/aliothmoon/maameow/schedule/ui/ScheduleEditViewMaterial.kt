package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.schedule.ui.ScheduleEditViewMaterial
import com.aliothmoon.maameow.schedule.ui.ScheduleEditViewMiuix
import com.aliothmoon.maameow.schedule.ui.ScheduleEditViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScheduleEditViewMaterial(
    navController: NavController,
    strategyId: String?,
    viewModel: ScheduleEditViewModel = koinViewModel()
) {
    if (isMiuixUi) {
        ScheduleEditViewMiuix(navController, strategyId, viewModel)
    } else {
        ScheduleEditViewMaterial(navController, strategyId, viewModel)
    }
}
