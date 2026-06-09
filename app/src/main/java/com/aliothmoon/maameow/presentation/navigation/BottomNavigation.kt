package com.aliothmoon.maameow.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.theme.MaaDesignTokens

sealed class BottomNavTab(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object HOME : BottomNavTab(
        route = Routes.HOME,
        labelRes = R.string.bottom_nav_home,
        icon = Icons.Default.Home
    )

    data object BACKGROUND : BottomNavTab(
        route = Routes.BACKGROUND_TASK,
        labelRes = R.string.bottom_nav_background_task,
        icon = Icons.Default.PlayArrow
    )

    data object SCHEDULE : BottomNavTab(
        route = Routes.SCHEDULE,
        labelRes = R.string.bottom_nav_schedule,
        icon = Icons.Default.DateRange
    )

    data object SETTINGS : BottomNavTab(
        route = Routes.SETTINGS,
        labelRes = R.string.bottom_nav_settings,
        icon = Icons.Default.Settings
    )

    companion object {
        val all = listOf(HOME, BACKGROUND, SCHEDULE, SETTINGS)
    }
}

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    blurEnabled: Boolean = true,
    floating: Boolean = true,
    liquidGlass: Boolean = true,
    onTabSelected: (BottomNavTab) -> Unit
) {
    val barShape = RoundedCornerShape(if (floating) 28.dp else 0.dp)
    val containerColor = if (liquidGlass) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        color = Color.Transparent,
        shadowElevation = if (floating) 10.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    horizontal = if (floating) 18.dp else 0.dp,
                    vertical = if (floating) 10.dp else 0.dp
                )
        ) {
            if (!floating) {
                HorizontalDivider(
                    thickness = MaaDesignTokens.Separator.thickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(barShape)
                    .background(containerColor)
                    .then(
                        if (liquidGlass) {
                            Modifier.border(
                                width = MaaDesignTokens.Separator.thickness,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = barShape
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (blurEnabled && liquidGlass) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(22.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = if (floating) 8.dp else 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavTab.all.forEach { tab ->
                    val label = stringResource(tab.labelRes)
                    val selected = currentRoute == tab.route
                    val contentColor = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                            Icon(
                            imageVector = tab.icon,
                            contentDescription = label,
                            modifier = Modifier.size(20.dp),
                            tint = contentColor
                        )
                                Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

}
