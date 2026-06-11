package com.aliothmoon.maameow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.aliothmoon.maameow.ui.isMiuixUi
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Auto-switching typography: returns Miuix textStyles in Miuix mode,
 * Material3 typography otherwise.
 * Usage: ThemeTypography.bodyMedium instead of MaterialTheme.typography.bodyMedium
 */
object ThemeTypography {
    val displayLarge: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline1 else MaterialTheme.typography.displayLarge
    val displayMedium: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline1 else MaterialTheme.typography.displayMedium
    val displaySmall: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline1 else MaterialTheme.typography.displaySmall
    val headlineLarge: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline1 else MaterialTheme.typography.headlineLarge
    val headlineMedium: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline1 else MaterialTheme.typography.headlineMedium
    val headlineSmall: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline2 else MaterialTheme.typography.headlineSmall
    val titleLarge: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline1 else MaterialTheme.typography.titleLarge
    val titleMedium: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline2 else MaterialTheme.typography.titleMedium
    val titleSmall: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.headline2 else MaterialTheme.typography.titleSmall
    val bodyLarge: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.body1 else MaterialTheme.typography.bodyLarge
    val bodyMedium: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.body2 else MaterialTheme.typography.bodyMedium
    val bodySmall: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.body2 else MaterialTheme.typography.bodySmall
    val labelLarge: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.body1 else MaterialTheme.typography.labelLarge
    val labelMedium: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.body2 else MaterialTheme.typography.labelMedium
    val labelSmall: TextStyle
        @Composable get() = if (isMiuixUi) MiuixTheme.textStyles.body2 else MaterialTheme.typography.labelSmall
}
