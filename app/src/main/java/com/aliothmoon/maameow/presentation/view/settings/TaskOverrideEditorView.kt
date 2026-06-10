package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import com.aliothmoon.maameow.presentation.view.settings.TaskOverrideEditorViewMaterial
import com.aliothmoon.maameow.presentation.view.settings.TaskOverrideEditorViewMiuix
import com.aliothmoon.maameow.presentation.viewmodel.TaskOverrideEditorViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun TaskOverrideEditorView(
    navController: NavController,
    viewModel: TaskOverrideEditorViewModel = koinViewModel(),
) {
    if (isMiuixUi) {
        TaskOverrideEditorViewMiuix(navController, viewModel)
    } else {
        TaskOverrideEditorViewMaterial(navController, viewModel)
    }
}
