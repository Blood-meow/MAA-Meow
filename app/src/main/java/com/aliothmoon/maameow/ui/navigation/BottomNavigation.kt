package com.aliothmoon.maameow.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.component.bottombar.FloatingBottomBar
import com.aliothmoon.maameow.ui.component.bottombar.FloatingBottomBarItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aliothmoon.maameow.ui.theme.MaaDesignTokens
import com.aliothmoon.maameow.ui.theme.ThemeColors

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

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    blurEnabled: Boolean = true,
    floating: Boolean = true,
    liquidGlass: Boolean = true,
    backdrop: LayerBackdrop? = null,
    onTabSelected: (BottomNavTab) -> Unit
) {
    val miuix = isMiuixUi
    val effectiveFloating = floating || miuix
    val effectiveLiquidGlass = liquidGlass || miuix
    val barShape = if (effectiveFloating) CircleShape else RoundedCornerShape(0.dp)
    val containerColor = when {
        effectiveLiquidGlass && miuix -> ThemeColors.surfaceContainer.copy(alpha = 0.68f)
        effectiveLiquidGlass -> ThemeColors.surface.copy(alpha = 0.72f)
        else -> ThemeColors.surface
    }
    val horizontalPadding = if (effectiveFloating) if (miuix) 16.dp else 18.dp else 0.dp
    val verticalPadding = if (effectiveFloating) if (miuix) 8.dp else 10.dp else 0.dp

    if (miuix && effectiveFloating && backdrop != null && isRenderEffectSupported()) {
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingBottomBar(
                selectedIndex = {
                    BottomNavTab.all.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
                },
                onSelected = { index ->
                    onTabSelected(BottomNavTab.all[index])
                },
                backdrop = backdrop,
                tabsCount = BottomNavTab.all.size,
                isBlurEnabled = blurEnabled,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                BottomNavTab.all.forEach { tab ->
                    val label = stringResource(tab.labelRes)
                    val selected = currentRoute == tab.route
                    FloatingBottomBarItem(
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp)
                    ) {
                        Icon(
                            imageVector = if (miuix) tab.miuixIcon else tab.icon,
                            contentDescription = label,
                            tint = if (selected) MiuixTheme.colorScheme.onSurface else ThemeColors.onSurfaceVariant
                        )
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = if (selected) MiuixTheme.colorScheme.onSurface else ThemeColors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
            // Transparent spacer for gesture area — content shows through
            Spacer(modifier = Modifier.height(navBarHeight))
        }
    } else {
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Transparent,
                shadowElevation = if (effectiveFloating) if (miuix) 14.dp else 10.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                ) {
                if (!effectiveFloating) {
                    HorizontalDivider(
                        thickness = MaaDesignTokens.Separator.thickness,
                        color = ThemeColors.outlineVariant.copy(alpha = 0.3f)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(barShape)
                        .background(containerColor)
                        .then(
                            if (effectiveLiquidGlass) {
                                Modifier.border(
                                    width = MaaDesignTokens.Separator.thickness,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            ThemeColors.onSurface.copy(alpha = if (miuix) 0.24f else 0.18f),
                                            ThemeColors.onSurface.copy(alpha = if (miuix) 0.08f else 0.06f)
                                        )
                                    ),
                                    shape = barShape
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    if (blurEnabled && effectiveLiquidGlass) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .blur(if (miuix) 26.dp else 22.dp)
                                .background(ThemeColors.primary.copy(alpha = if (miuix) 0.08f else 0.06f))
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (miuix) 18.dp else 24.dp,
                                vertical = if (effectiveFloating) if (miuix) 10.dp else 8.dp else 6.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavTab.all.forEach { tab ->
                            val label = stringResource(tab.labelRes)
                            val selected = currentRoute == tab.route
                            val transitionSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
                            val animatedColor by animateColorAsState(
                                targetValue = if (selected) {
                                    ThemeColors.primary
                                } else {
                                    ThemeColors.onSurfaceVariant.copy(alpha = 0.7f)
                                },
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                label = "bottomNavContentColor"
                            )
                            val iconSize by animateDpAsState(
                                targetValue = if (miuix && selected) 24.dp else if (miuix) 22.dp else if (selected) 21.dp else 20.dp,
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                                label = "bottomNavIconSize"
                            )
                            val itemScale by animateFloatAsState(
                                targetValue = if (selected) 1.04f else 1f,
                                animationSpec = transitionSpec,
                                label = "bottomNavItemScale"
                            )

                            Column(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = itemScale
                                        scaleY = itemScale
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onTabSelected(tab) }
                                    .heightIn(min = if (miuix) 52.dp else 48.dp)
                                    .padding(horizontal = if (miuix) 14.dp else 20.dp, vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(if (miuix) 1.dp else 0.dp)
                            ) {
                                Icon(
                                    imageVector = if (miuix) tab.miuixIcon else tab.icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(iconSize),
                                    tint = animatedColor
                                )
                                AnimatedVisibility(
                                    visible = !miuix || selected,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 160)) +
                                        scaleIn(initialScale = 0.92f, animationSpec = transitionSpec),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 120)) +
                                        scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = 120))
                                ) {
                                    Text(
                                        text = label,
                                        style = if (miuix) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                        fontWeight = if (miuix && selected) FontWeight.Medium else FontWeight.Normal,
                                        color = animatedColor
                                    )
                                }
                }
                }
            }
            // Transparent spacer for gesture area
            Spacer(modifier = Modifier.height(navBarHeight))
        }
    }
}
        }
    }
}