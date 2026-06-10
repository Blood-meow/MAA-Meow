package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.viewmodel.TaskOverrideEditorViewModel


@Composable
fun TaskOverrideEditorViewMiuix(
    navController: NavController, viewModel: TaskOverrideEditorViewModel
) {
    TaskOverrideEditorViewMaterial(navController, viewModel)
}
