package com.aliothmoon.maameow.ui.screen.panel.mall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import com.aliothmoon.maameow.ui.component.material.MaaUiCardContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import com.aliothmoon.maameow.ui.component.material.MaaUiIconButton
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PriorityItemRow(
    item: String,
    isDragging: Boolean,
    isReorderMode: Boolean,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaaUiCardContainer(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDragging -> ThemeColors.surfaceVariant
                isReorderMode -> ThemeColors.primaryContainer.copy(alpha = 0.1f)
                enabled -> ThemeColors.surface
                else -> ThemeColors.surfaceVariant
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isDragging -> ThemeColors.primary
                isReorderMode -> ThemeColors.primary.copy(alpha = 0.5f)
                else -> ThemeColors.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isReorderMode,
                enter = fadeIn(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Menu,
                        stringResource(R.string.panel_mall_drag_reorder),
                        modifier = Modifier
                            .size(28.dp)
                            .padding(5.dp),
                        tint = ThemeColors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            MaaUiText(
                item,
                style = ThemeTypography.bodyMedium,
                color = if (enabled) ThemeColors.onSurface else ThemeColors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            AnimatedVisibility(visible = !isReorderMode) {
                MaaUiIconButton(
                    onClick = onRemove,
                    enabled = enabled && !isDragging,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_delete),
                        modifier = Modifier.size(16.dp),
                        tint = if (enabled) ThemeColors.error else ThemeColors.outlineVariant
                    )
                }
            }
        }
    }
}
