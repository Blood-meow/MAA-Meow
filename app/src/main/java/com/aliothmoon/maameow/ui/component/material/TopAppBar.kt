package com.aliothmoon.maameow.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.aliothmoon.maameow.ui.component.material.MaaUiTopAppBar

@Composable
fun TopAppBar(
    title: String,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actionIcon: ImageVector? = null,
    actionIconDescription: String? = null,
    onActionClick: () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    MaaUiTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        onNavigationClick = onNavigationClick,
        actionIcon = actionIcon,
        actionIconDescription = actionIconDescription,
        onActionClick = onActionClick,
        actions = actions
    )
}
