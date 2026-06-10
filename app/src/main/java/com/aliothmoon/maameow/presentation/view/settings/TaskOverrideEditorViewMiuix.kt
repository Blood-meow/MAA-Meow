package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.view.settings.TaskOverrideEditorViewMaterial
import org.koin.androidx.compose.koinViewModel

@Composable
fun TaskOverrideEditorViewMiuix(
    navController: NavController,
    viewModel: TaskOverrideEditorViewModel = koinViewModel(),
) {
    TaskOverrideEditorViewMaterial(navController, viewModel)
}
