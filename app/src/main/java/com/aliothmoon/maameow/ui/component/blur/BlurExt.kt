package com.aliothmoon.maameow.ui.component.blur

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.ui.component.liquid.lens
import com.aliothmoon.maameow.ui.component.liquid.vibrancy
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Creates a [LayerBackdrop] for blur effects if blur is enabled and supported.
 * Returns null when blur should be disabled (e.g. device doesn't support RenderEffect).
 */
@Composable
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * Wraps a composable (typically a TopAppBar) with a frosted-glass blur effect.
 * Uses the legacy textureBlur API.
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}

/**
 * Wraps a composable with a liquid-glass blur effect using the new drawBackdrop API.
 * Includes vibrancy + subtle lens refraction for a premium frosted-glass look.
 *
 * @param backdrop The LayerBackdrop to draw into (null = no blur, fallback to solid)
 */
@Composable
fun LiquidGlassBar(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    if (backdrop != null) {
        val surfaceTint = MiuixTheme.colorScheme.surface.copy(0.6f)
        Box(
            modifier = Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx(), 8.dp.toPx())
                    lens(
                        refractionHeight = 12.dp.toPx(),
                        refractionAmount = 8.dp.toPx(),
                    )
                },
                onDrawSurface = {
                    drawRect(surfaceTint)
                },
            )
        ) {
            content()
        }
    } else {
        Box {
            content()
        }
    }
}