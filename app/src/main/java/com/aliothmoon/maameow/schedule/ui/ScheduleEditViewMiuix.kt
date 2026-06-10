package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.schedule.ui.ScheduleEditViewMaterial
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScheduleEditViewMiuix(
    navController: NavController,
    strategyId: String?,
    viewModel: ScheduleEditViewModel = koinViewModel()
) {
    ScheduleEditViewMaterial(navController, strategyId, viewModel)
}
