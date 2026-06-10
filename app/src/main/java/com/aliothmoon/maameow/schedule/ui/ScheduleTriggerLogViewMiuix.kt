package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController


@Composable
fun ScheduleTriggerLogViewMiuix(
    navController: NavController, viewModel: ScheduleTriggerLogViewModel
) {
    ScheduleTriggerLogViewMaterial(navController, viewModel)
}
