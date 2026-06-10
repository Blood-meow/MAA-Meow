package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogViewMaterial
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogViewMiuix
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScheduleTriggerLogViewMaterial(
    navController: NavController,
    viewModel: ScheduleTriggerLogViewModel = koinViewModel(),
) {
    if (isMiuixUi) {
        ScheduleTriggerLogViewMiuix(navController, viewModel)
    } else {
        ScheduleTriggerLogViewMaterial(navController, viewModel)
    }
}
