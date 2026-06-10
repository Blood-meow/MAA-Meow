package com.aliothmoon.maameow.schedule.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScheduleListView(
    navController: NavController,
    viewModel: ScheduleListViewModel = koinViewModel(),
) {
    if (isMiuixUi) {
        ScheduleListViewMiuix(navController, viewModel)
    } else {
        ScheduleListViewMaterial(navController, viewModel)
    }
}
