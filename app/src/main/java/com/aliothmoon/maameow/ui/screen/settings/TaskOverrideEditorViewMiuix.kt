package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.viewmodel.TaskOverrideEditorViewModel

@Composable
fun TaskOverrideEditorViewMiuix(
    navController: NavController, viewModel: TaskOverrideEditorViewModel
) {
    // Code editor view — inner content uses sora-editor AndroidView which has no Miuix equivalent.
    TaskOverrideEditorViewMaterial(navController, viewModel)
}
