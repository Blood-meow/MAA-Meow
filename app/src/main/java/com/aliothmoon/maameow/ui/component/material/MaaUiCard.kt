package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.theme.MaaDesignTokens
import com.aliothmoon.maameow.ui.theme.ThemeColors

@Composable
fun MaaUiCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = PaddingValues(MaaDesignTokens.Card.innerPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val miuix = isMiuixUi
    val shape = if (miuix) RoundedCornerShape(22.dp) else MaterialTheme.shapes.medium
    val background = if (miuix) {
        ThemeColors.surfaceContainer.copy(alpha = 0.72f)
    } else {
        containerColor
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        elevation = CardDefaults.cardElevation(defaultElevation = if (miuix) 0.dp else MaaDesignTokens.Card.elevation),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = if (miuix) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                    color = if (miuix) ThemeColors.onSurface else contentColor,
                    modifier = Modifier.padding(bottom = MaaDesignTokens.Spacing.sm)
                )
            }
            content()
        }
    }
}
