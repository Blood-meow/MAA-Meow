package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogViewMaterial
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScheduleTriggerLogViewMiuix(
    navController: NavController,
    viewModel: ScheduleTriggerLogViewModel = koinViewModel(),
) {
    ScheduleTriggerLogViewMaterial(navController, viewModel)
}
