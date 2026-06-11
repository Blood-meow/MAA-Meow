package com.aliothmoon.maameow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aliothmoon.maameow.ui.LocalIsPureDark
import com.aliothmoon.maameow.ui.isMiuixUi
import top.yukonga.miuix.kmp.theme.MiuixTheme

object ThemeColors {
    val background: Color
        @Composable get() {
            val pureDark = LocalIsPureDark.current
            if (pureDark) return Color.Black
            return if (isMiuixUi) MiuixTheme.colorScheme.background else MaterialTheme.colorScheme.background
        }

    val surface: Color
        @Composable get() {
            val pureDark = LocalIsPureDark.current
            if (pureDark) return Color.Black
            return if (isMiuixUi) MiuixTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
        }

    val surfaceContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainer

    val surfaceVariant: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant

    val onSurface: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface

    val onSurfaceVariant: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.onSurfaceSecondary else MaterialTheme.colorScheme.onSurfaceVariant

    val primary: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.primary

    val onPrimary: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary

    val primaryContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer

    val onPrimaryContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer

    val secondary: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary

    val secondaryContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.secondaryContainer

    val onSecondaryContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer

    val tertiary: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    val tertiaryContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer

    val onTertiaryContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onTertiaryContainer

    val error: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.error else MaterialTheme.colorScheme.error

    val errorContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer

    val onErrorContainer: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer

    val outline: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.outline else MaterialTheme.colorScheme.outline

    val outlineVariant: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant

    val scrim: Color
        @Composable get() = if (isMiuixUi) Color.Black.copy(alpha = 0.32f) else MaterialTheme.colorScheme.scrim
}