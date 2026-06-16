package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.screen.settings.TaskOverrideEditorViewMaterial
import com.aliothmoon.maameow.ui.screen.settings.TaskOverrideEditorViewMiuix
import com.aliothmoon.maameow.ui.viewmodel.TaskOverrideEditorViewModel
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
