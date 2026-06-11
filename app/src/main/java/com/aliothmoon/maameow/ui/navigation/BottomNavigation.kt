package com.aliothmoon.maameow.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.component.bottombar.FloatingBottomBarAdvanced
import com.aliothmoon.maameow.ui.component.bottombar.FloatingBottomBarItemAdvanced
import com.aliothmoon.maameow.ui.component.blur.BlurredBar
import com.aliothmoon.maameow.ui.component.blur.rememberBlurBackdrop
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.theme.MaaDesignTokens
import com.aliothmoon.maameow.ui.theme.ThemeTypography
import com.aliothmoon.maameow.ui.theme.ThemeColors
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

sealed class BottomNavTab(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val miuixIcon: ImageVector = icon
) {
    data object HOME : BottomNavTab(
        route = Routes.HOME,
        labelRes = R.string.bottom_nav_home,
        icon = Icons.Default.Home,
        miuixIcon = Icons.Rounded.Cottage
    )

    data object BACKGROUND : BottomNavTab(
        route = Routes.BACKGROUND_TASK,
        labelRes = R.string.bottom_nav_background_task,
        icon = Icons.Default.PlayArrow,
        miuixIcon = Icons.Rounded.PlayArrow
    )

    data object SCHEDULE : BottomNavTab(
        route = Routes.SCHEDULE,
        labelRes = R.string.bottom_nav_schedule,
        icon = Icons.Default.DateRange,
        miuixIcon = Icons.Rounded.DateRange
    )

    data object SETTINGS : BottomNavTab(
        route = Routes.SETTINGS,
        labelRes = R.string.bottom_nav_settings,
        icon = Icons.Default.Settings,
        miuixIcon = Icons.Rounded.Settings
    )

    companion object {
        val all = listOf(HOME, BACKGROUND, SCHEDULE, SETTINGS)
    }
}

/**
 * Material-style bottom navigation bar (non-floating, standard row of icons).
 */
@Composable
private fun AppBottomNavigationMaterial(
    currentRoute: String,
    onTabSelected: (BottomNavTab) -> Unit
) {
    MaaUiSurface(
        color = ThemeColors.surface,
        shadowElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = MaaDesignTokens.Separator.thickness,
                color = ThemeColors.outlineVariant.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTab.all.forEach { tab ->
                    val label = stringResource(tab.labelRes)
                    val selected = currentRoute == tab.route
                    val contentColor = if (selected) ThemeColors.primary
                    else ThemeColors.onSurfaceVariant.copy(alpha = 0.7f)

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
                        MaaUiText(
                            text = label,
                            style = ThemeTypography.labelMedium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Miuix-style bottom navigation: FloatingBottomBar when enabled, else standard NavigationBar.
 */
@Composable
fun MiuixBottomNavigation(
    currentRoute: String,
    onTabSelected: (BottomNavTab) -> Unit,
    backdrop: Backdrop? = null,
    appSettings: AppSettingsManager = koinInject(),
) {
    val selectedIndexInt = when (currentRoute) {
        Routes.HOME -> 0
        Routes.BACKGROUND_TASK -> 1
        Routes.SCHEDULE -> 2
        Routes.SETTINGS -> 3
        else -> 0
    }

    val enableFloatingBottomBar by appSettings.uiFloatingBottomBar.collectAsStateWithLifecycle()
    val enableBlur by appSettings.uiBlurEnabled.collectAsStateWithLifecycle()

    if (enableFloatingBottomBar && backdrop != null) {
        FloatingBottomBarAdvanced(
            selectedIndex = { selectedIndexInt },
            onSelected = { idx -> onTabSelected(BottomNavTab.all[idx]) },
            backdrop = backdrop,
            tabsCount = BottomNavTab.all.size,
            isBlurEnabled = enableBlur,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            BottomNavTab.all.forEach { tab ->
                val label = stringResource(tab.labelRes)
                val selected = currentRoute == tab.route
                FloatingBottomBarItemAdvanced(
                    onClick = { onTabSelected(tab) },
                ) {
                    Icon(
                        imageVector = tab.miuixIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    MaaUiText(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) MiuixTheme.colorScheme.primary
                        else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    } else {
        // Standard miuix NavigationBar with optional blur
        val blurBackdrop = rememberBlurBackdrop(enableBlur = enableBlur)
        val destinations = BottomNavTab.all
        val items = destinations.map { tab ->
            NavigationItem(
                label = stringResource(tab.labelRes),
                icon = tab.miuixIcon,
            )
        }
        BlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = Modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        icon = item.icon,
                        label = item.label,
                        selected = selectedIndexInt == index,
                        onClick = {
                            onTabSelected(BottomNavTab.all[index])
                        }
                    )
                }
            }
        }
    }
}

/**
 * Main entry point: delegates to Material or Miuix based on UI mode.
 */
@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onTabSelected: (BottomNavTab) -> Unit,
    backdrop: Backdrop? = null
) {
    if (isMiuixUi) {
        MiuixBottomNavigation(
            currentRoute = currentRoute,
            onTabSelected = onTabSelected,
            backdrop = backdrop
        )
    } else {
        AppBottomNavigationMaterial(
            currentRoute = currentRoute,
            onTabSelected = onTabSelected
        )
    }
}