package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aliothmoon.maameow.ui.navigation.LocalFloatingBottomBarHeight
import com.aliothmoon.maameow.ui.theme.ThemeColors
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Hidden viewer: Miuix theme color tokens displayed as color swatches.
 *
 * Layout: rows of two swatches. Each swatch column has the color block
 * on top and the token name + hex on the bottom (left-aligned, plain
 * onSurface text).
 */
@Composable
fun MonetColorsViewMiuix(navController: NavController) {
    // Bind all ThemeColors properties first (they're @Composable getters);
    // then build the immutable swatch list outside any composable scope.
    val primary             = ThemeColors.primary
    val onPrimary           = ThemeColors.onPrimary
    val primaryContainer    = ThemeColors.primaryContainer
    val onPrimaryContainer  = ThemeColors.onPrimaryContainer
    val secondary           = ThemeColors.secondary
    val secondaryContainer  = ThemeColors.secondaryContainer
    val onSecondaryContainer= ThemeColors.onSecondaryContainer
    val tertiary            = ThemeColors.tertiary
    val tertiaryContainer   = ThemeColors.tertiaryContainer
    val onTertiaryContainer = ThemeColors.onTertiaryContainer
    val error               = ThemeColors.error
    val errorContainer      = ThemeColors.errorContainer
    val onErrorContainer    = ThemeColors.onErrorContainer
    val background          = ThemeColors.background
    val onBackground        = ThemeColors.onBackground
    val surface             = ThemeColors.surface
    val onSurface           = ThemeColors.onSurface
    val surfaceVariant      = ThemeColors.surfaceVariant
    val onSurfaceVariant    = ThemeColors.onSurfaceVariant
    val outline             = ThemeColors.outline
    val outlineVariant      = ThemeColors.outlineVariant
    val scrim               = ThemeColors.scrim

    val swatches = listOf(
        MonetSwatch("primary",            "主题色",       primary),
        MonetSwatch("onPrimary",          "主色字",       onPrimary),
        MonetSwatch("primaryContainer",   "主容器",       primaryContainer),
        MonetSwatch("onPrimaryContainer", "主容器字",     onPrimaryContainer),
        MonetSwatch("secondary",          "次色",         secondary),
        MonetSwatch("secondaryContainer", "次容器",       secondaryContainer),
        MonetSwatch("onSecondaryContainer","次容器字",    onSecondaryContainer),
        MonetSwatch("tertiary",           "第三色",       tertiary),
        MonetSwatch("tertiaryContainer",  "第三容器",     tertiaryContainer),
        MonetSwatch("onTertiaryContainer","第三容器字",   onTertiaryContainer),
        MonetSwatch("error",              "错误色",       error),
        MonetSwatch("errorContainer",     "错误容器",     errorContainer),
        MonetSwatch("onErrorContainer",   "错误容器字",   onErrorContainer),
        MonetSwatch("background",         "背景",         background),
        MonetSwatch("onBackground",       "背景字",       onBackground),
        MonetSwatch("surface",            "表面",         surface),
        MonetSwatch("onSurface",          "表面字",       onSurface),
        MonetSwatch("surfaceVariant",     "表面变体",     surfaceVariant),
        MonetSwatch("onSurfaceVariant",   "表面变体字",   onSurfaceVariant),
        MonetSwatch("outline",            "描边",         outline),
        MonetSwatch("outlineVariant",     "描边变体",     outlineVariant),
        MonetSwatch("scrim",              "遮罩",         scrim),
    )

    MiuixScaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MiuixTopAppBar(
                title = "Monet Colors",
                color = MiuixTheme.colorScheme.surface,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = LocalFloatingBottomBarHeight.current +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(swatches.chunked(2)) { rowSwatches ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSwatches.forEach { swatch ->
                        MonetSwatchColumn(
                            swatch = swatch,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowSwatches.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class MonetSwatch(
    val name: String,
    val label: String,
    val color: Color,
)

@Composable
private fun MonetSwatchColumn(swatch: MonetSwatch, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(swatch.color),
        )
        Spacer(modifier = Modifier.height(6.dp))
        MiuixText(
            text = "${swatch.name} ${swatch.label}",
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        MiuixText(
            text = colorToHex(swatch.color),
            color = ThemeColors.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    val a = (argb ushr 24) and 0xFF
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}