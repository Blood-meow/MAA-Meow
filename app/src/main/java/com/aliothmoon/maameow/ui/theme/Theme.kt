package com.aliothmoon.maameow.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.ui.LocalIsPureDark
import com.aliothmoon.maameow.ui.LocalUiStyle
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private val LightBackground = Color(0xFFF5F2ED)
private val LightSurface = Color(0xFFF9F7F3)
private val LightSurfaceVariant = Color(0xFFE8E4DE)
private val LightOnSurface = Color(0xFF1C1B18)
private val LightOnSurfaceVariant = Color(0xFF8A8580)
private val LightOutline = Color(0xFFC9C4BE)

private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1C1C1E)
private val DarkSurfaceVariant = Color(0xFF2C2C2E)
private val DarkOnSurface = Color(0xFFFFFFFF)
private val DarkOnSurfaceVariant = Color(0xFF98989D)
private val DarkOutline = Color(0xFF3A3A3C)

private val PureDarkBackground = Color(0xFF000000)
private val PureDarkSurface = Color(0xFF000000)
private val PureDarkSurfaceVariant = Color(0xFF121212)


private fun createLightColorScheme(
    primary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color
): ColorScheme {
    return lightColorScheme(
        primary = primary,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = Color(0xFF8A8580),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8E4DE),
        onSecondaryContainer = Color(0xFF1C1B18),
        tertiary = primary.copy(alpha = 0.8f),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = primaryContainer.copy(alpha = 0.5f),
        onTertiaryContainer = onPrimaryContainer,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outline = LightOutline,
        outlineVariant = LightSurfaceVariant,
        error = Color(0xfff53f3f),
        onError = Color.White,
        errorContainer = Color(0xFFFFD8D6),
        onErrorContainer = Color(0xFF690005)
    )
}

private fun createDarkColorScheme(
    primary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    isPureDark: Boolean = false
): ColorScheme {
    val bg = if (isPureDark) PureDarkBackground else DarkBackground
    val surface = if (isPureDark) PureDarkSurface else DarkSurface
    val surfaceVariant = if (isPureDark) PureDarkSurfaceVariant else DarkSurfaceVariant

    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = Color(0xFF98989D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF2C2C2E),
        onSecondaryContainer = Color(0xFFE5E5EA),
        tertiary = primary.copy(alpha = 0.8f),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = primaryContainer.copy(alpha = 0.5f),
        onTertiaryContainer = onPrimaryContainer,
        background = bg,
        onBackground = DarkOnSurface,
        surface = surface,
        onSurface = DarkOnSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outline = DarkOutline,
        outlineVariant = surfaceVariant,
        error = Color(0xFFFF453A),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6)
    )
}

private val BlueLight = createLightColorScheme(
    primary = Color(0xFF2B6BCA),
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF002453)
)
private val BlueDark = createDarkColorScheme(
    primary = Color(0xFF2B6BCA),
    primaryContainer = Color(0xFF004088),
    onPrimaryContainer = Color(0xFFD6E8FF)
)
private val BluePureDark = createDarkColorScheme(
    primary = Color(0xFF2B6BCA),
    primaryContainer = Color(0xFF004088),
    onPrimaryContainer = Color(0xFFD6E8FF),
    isPureDark = true
)

val MaaShapes = Shapes(
    extraSmall = RoundedCornerShape(MaaDesignTokens.CornerRadius.inner),
    small = RoundedCornerShape(MaaDesignTokens.CornerRadius.button),
    medium = RoundedCornerShape(MaaDesignTokens.CornerRadius.card),
    large = RoundedCornerShape(MaaDesignTokens.CornerRadius.card),
    extraLarge = RoundedCornerShape(MaaDesignTokens.CornerRadius.pill)
)


private object NoIndication : IndicationNodeFactory {
    private class NoIndicationNode : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            drawContent()
        }
    }

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return NoIndicationNode()
    }

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}

object MaaThemeAlphas {
    const val Disabled = 0.38f
    const val Secondary = 0.60f
    const val Medium = 0.74f
}

@Composable
fun MaaMeowTheme(
    themeMode: AppSettingsManager.ThemeMode = AppSettingsManager.ThemeMode.SYSTEM,
    monetEnabled: Boolean = false,
    uiStyle: AppSettingsManager.UiStyle = AppSettingsManager.UiStyle.MATERIAL,
    keyColor: Long = 0L,
    fontSizeScale: Int = 100,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        AppSettingsManager.ThemeMode.SYSTEM -> systemDarkTheme
        AppSettingsManager.ThemeMode.WHITE -> false
        AppSettingsManager.ThemeMode.DARK,
        AppSettingsManager.ThemeMode.PURE_DARK -> true
    }
    val isPureDark = themeMode == AppSettingsManager.ThemeMode.PURE_DARK
    val seedColor = if (keyColor != 0L) Color(keyColor.toInt()) else null
    val colorScheme = if (monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && seedColor == null) {
        // System dynamic colors (wallpaper-based), no custom key color
        val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (isPureDark) {
            dynamic.copy(background = Color(0xFF000000), surface = Color(0xFF000000), surfaceVariant = Color(0xFF121212))
        } else dynamic
    } else if (seedColor != null && monetEnabled) {
        // Custom key color with monet — generate scheme from seed
        val base = if (darkTheme) {
            createDarkColorScheme(primary = seedColor, primaryContainer = seedColor.copy(alpha = 0.3f), onPrimaryContainer = seedColor, isPureDark = isPureDark)
        } else {
            createLightColorScheme(primary = seedColor, primaryContainer = seedColor.copy(alpha = 0.15f), onPrimaryContainer = seedColor)
        }
        base
    } else {
        when (themeMode) {
            AppSettingsManager.ThemeMode.SYSTEM -> if (systemDarkTheme) BlueDark else BlueLight
            AppSettingsManager.ThemeMode.WHITE -> BlueLight
            AppSettingsManager.ThemeMode.DARK -> BlueDark
            AppSettingsManager.ThemeMode.PURE_DARK -> BluePureDark
        }
    }

    val miuixMode = if (monetEnabled) {
        when (themeMode) {
            AppSettingsManager.ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            AppSettingsManager.ThemeMode.WHITE -> ColorSchemeMode.MonetLight
            AppSettingsManager.ThemeMode.DARK,
            AppSettingsManager.ThemeMode.PURE_DARK -> ColorSchemeMode.MonetDark
        }
    } else {
        when (themeMode) {
            AppSettingsManager.ThemeMode.SYSTEM -> ColorSchemeMode.System
            AppSettingsManager.ThemeMode.WHITE -> ColorSchemeMode.Light
            AppSettingsManager.ThemeMode.DARK,
            AppSettingsManager.ThemeMode.PURE_DARK -> ColorSchemeMode.Dark
        }
    }
    val miuixKeyColor = when {
        !monetEnabled -> null
        keyColor != 0L -> Color(keyColor.toInt())
        else -> null  // null = use system wallpaper dynamic colors (MonetSystem)
    }
    val miuixController = remember(miuixMode, miuixKeyColor) {
        ThemeController(
            colorSchemeMode = miuixMode,
            keyColor = miuixKeyColor
        )
    }

    val scaledDensity = LocalDensity.current.let { Density(it.density, it.fontScale * fontSizeScale / 100f) }

    // Only suppress Material ripple in Miuix mode; Miuix has its own press effects.
    // In Material mode, let MaterialTheme's default ripple indication work.
    val indicationOverride = if (uiStyle == AppSettingsManager.UiStyle.MIUIX)
        NoIndication else null

    // miuix 0.9.2 internally calls Android 13+ RenderEffect / RuntimeShader APIs
    // that are not available on Android 9-12. Gate the Miuix wrapper behind a
    // SDK check so older devices (where the UI Style entry is also hidden) get
    // a pure Material3 theme with no miuix initialization.
    val canUseMiuix = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    if (canUseMiuix) {
    MiuixTheme(controller = miuixController) {
        CompositionLocalProvider(
            *if (indicationOverride != null) arrayOf(LocalIndication provides indicationOverride)
            else emptyArray()
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = MaaShapes,
            ) {
                // In Miuix mode, bridge Miuix onSurface → Material3 LocalContentColor
                val miuixOnSurface = if (uiStyle == AppSettingsManager.UiStyle.MIUIX)
                    MiuixTheme.colorScheme.onSurface else null
                CompositionLocalProvider(
                    LocalUiStyle provides uiStyle,
                    LocalIsPureDark provides isPureDark,
                    LocalDensity provides scaledDensity,
                    *(if (miuixOnSurface != null) arrayOf(
                        LocalContentColor provides miuixOnSurface,
                        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
                    )
                      else emptyArray())
                ) {
                    val miuixPrimary = if (uiStyle == AppSettingsManager.UiStyle.MIUIX) {
                        // In Miuix mode, the Miuix controller above may end up with
                        // keyColor = null when monetEnabled = true and the user has
                        // selected "system Monet" (uiKeyColor = 0L). Miuix then derives
                        // its own primary from the wallpaper, but its built-in default
                        // is a hardcoded blue (#FF2B6BCA) when the controller doesn't
                        // pick up a real Monet color. Force Miuix primary to the same
                        // scheme Material uses (Monet dynamic or seed-derived) so all
                        // Miuix surfaces (bottom bar selected indicator, buttons,
                        // selection borders, ...) actually follow the user's choice
                        // instead of falling back to that hardcoded blue.
                        when {
                            seedColor != null -> seedColor
                            monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                if (darkTheme) dynamicDarkColorScheme(context).primary
                                else dynamicLightColorScheme(context).primary
                            }
                            isPureDark -> Color(0xFF2B6BCA)
                            else -> MiuixTheme.colorScheme.primary
                        }
                    } else null
                    val overrideColors = if (miuixPrimary != null) {
                        MiuixTheme.colorScheme.copy(
                            background = when {
                                isPureDark -> Color(0xFF000000)
                                darkTheme  -> Color(0xFF121212)
                                else       -> Color(0xFFF5F2ED)
                            },
                            surface = when {
                                isPureDark -> Color(0xFF000000)
                                darkTheme  -> Color(0xFF121212)
                                else       -> Color(0xFFF9F7F3)
                            },
                            surfaceVariant = when {
                                isPureDark -> Color(0xFF121212)
                                darkTheme  -> Color(0xFF2A2A2C)
                                else       -> Color(0xFFE8E4DE)
                            },
                            primary = miuixPrimary,
                            onPrimary = Color(0xFFFFFFFF),
                        )
                    } else {
                        MiuixTheme.colorScheme
                    }
                    MiuixTheme(colors = overrideColors) {
                        content()
                    }
                }
            }
        }
    }
    } else {
        // Android < 13: miuix is unavailable, fall back to pure Material3.
        // LocalUiStyle / LocalIsPureDark / LocalDensity are still provided so
        // child composables that read them keep working.
        CompositionLocalProvider(
            LocalUiStyle provides uiStyle,
            LocalIsPureDark provides isPureDark,
            LocalDensity provides scaledDensity
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = MaaShapes,
                content = content
            )
        }
    }
}

@Composable
fun MaterialThemeWrapper(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIndication provides NoIndication) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MaaShapes,
            content = content
        )
    }
}

@Composable
fun MiuixThemeWrapper(
    controller: ThemeController,
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    MiuixTheme(controller = controller) {
        MaterialThemeWrapper(
            colorScheme = colorScheme,
            content = content
        )
    }
}
