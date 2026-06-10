package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.ui.theme.ThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaaUiTopAppBar(
    title: String,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actionIcon: ImageVector? = null,
    actionIconDescription: String? = null,
    onActionClick: () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = if (isMiuixUi) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium
        )
    }
    val navigationContent: @Composable () -> Unit = {
        navigationIcon?.let { icon ->
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.accessibility_navigation)
                )
            }
        }
    }
    val actionsContent: @Composable RowScope.() -> Unit = {
        if (actions != null) {
            actions()
        } else {
            actionIcon?.let { icon ->
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = icon,
                        contentDescription = actionIconDescription
                    )
                }
            }
        }
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = ThemeColors.background,
        titleContentColor = ThemeColors.onSurface,
        navigationIconContentColor = ThemeColors.primary,
        actionIconContentColor = ThemeColors.primary
    )
    if (isMiuixUi) {
        LargeTopAppBar(
            title = titleContent,
            navigationIcon = navigationContent,
            actions = actionsContent,
            colors = colors
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationContent,
            actions = actionsContent,
            colors = colors
        )
    }
}
