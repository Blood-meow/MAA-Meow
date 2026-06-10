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

    val outline: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.outline else MaterialTheme.colorScheme.outline

    val outlineVariant: Color
        @Composable get() = if (isMiuixUi) MiuixTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
}