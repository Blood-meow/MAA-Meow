package com.aliothmoon.maameow.presentation.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.aliothmoon.maameow.presentation.components.ui.MaaUiCard
import com.aliothmoon.maameow.theme.MaaDesignTokens

@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
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
