package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun ScheduleListViewMiuix(
    navController: NavController,
    viewModel: ScheduleListViewModel
) {
    ScheduleListViewMaterial(navController, viewModel)
}
