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
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MaaUiCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = ThemeColors.surface,
    contentColor: Color = ThemeColors.onSurface,
    contentPadding: PaddingValues = PaddingValues(MaaDesignTokens.Card.innerPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val miuix = isMiuixUi
    if (miuix) {
        MiuixSurface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                if (title.isNotEmpty()) {
                    MiuixText(
                        text = title,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = MaaDesignTokens.Spacing.sm)
                    )
                }
                content()
            }
        }
    } else {
        val shape = MaterialTheme.shapes.medium
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape),
            elevation = CardDefaults.cardElevation(defaultElevation = MaaDesignTokens.Card.elevation),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = MaaDesignTokens.Spacing.sm)
                    )
                }
                content()
            }
        }
    }
}
