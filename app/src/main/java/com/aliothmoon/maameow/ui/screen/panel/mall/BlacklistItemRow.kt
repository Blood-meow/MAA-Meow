package com.aliothmoon.maameow.ui.screen.panel.mall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
fun BlacklistItemRow(item: String, enabled: Boolean, onRemove: () -> Unit) {
    MaaUiCardContainer(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) ThemeColors.surface else ThemeColors.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColors.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaaUiText(
                item,
                style = ThemeTypography.bodyMedium,
                color = if (enabled) ThemeColors.onSurface else ThemeColors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            // 删除按钮
            MaaUiIconButton(
                onClick = onRemove,
                enabled = enabled,
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
