package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController


@Composable
fun ScheduleEditViewMiuix(
    navController: NavController, strategyId: String?, viewModel: ScheduleEditViewModel
) {
    ScheduleEditViewMaterial(navController, strategyId, viewModel)
}
