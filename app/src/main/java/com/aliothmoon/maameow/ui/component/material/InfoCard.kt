package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aliothmoon.maameow.ui.component.material.MaaUiCard
import com.aliothmoon.maameow.ui.theme.MaaDesignTokens
import com.aliothmoon.maameow.ui.theme.ThemeColors

@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = ThemeColors.surface,
    contentColor: Color = ThemeColors.onSurface,
    contentPadding: PaddingValues = PaddingValues(MaaDesignTokens.Card.innerPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    MaaUiCard(
        title = title,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        contentPadding = contentPadding,
        content = content
    )
}
