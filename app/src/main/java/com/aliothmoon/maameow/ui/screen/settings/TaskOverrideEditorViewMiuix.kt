package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.viewmodel.TaskOverrideEditorViewModel


@Composable
fun TaskOverrideEditorViewMiuix(
    navController: NavController, viewModel: TaskOverrideEditorViewModel
) {
    TaskOverrideEditorViewMaterial(navController, viewModel)
}
