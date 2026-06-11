package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

/**
 * 通用可选芯片按钮组
 * 使用 FlowRow 自动换行平铺显示选项
 *
 * 颜色规范:
 * - 选中: primary / onPrimary
 * - 未选中: outline(0.3) / onSurface
 * - 禁用: surfaceVariant(0.38) / onSurface(0.25)
 * - 选中+全局禁用: primary(0.5) / onPrimary
 */
@Composable
fun <T> SelectableChipGroup(
    label: String,
    selectedValue: T,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isItemEnabled: (T) -> Boolean = { true },
    labelFontWeight: FontWeight? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        MaaUiText(
            text = label,
            style = ThemeTypography.bodySmall,
            fontWeight = labelFontWeight,
            color = ThemeColors.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { (value, displayName) ->
                val isSelected = value == selectedValue
                val itemEnabled = enabled && isItemEnabled(value)
                val chipColor = when {
                    isSelected && !enabled -> ThemeColors.primary.copy(alpha = 0.5f)
                    isSelected -> ThemeColors.primary
                    !itemEnabled -> ThemeColors.surfaceVariant.copy(alpha = 0.38f)
                    else -> ThemeColors.outline.copy(alpha = 0.3f)
                }
                val textColor = when {
                    isSelected -> ThemeColors.onPrimary
                    !itemEnabled -> ThemeColors.onSurface.copy(alpha = 0.25f)
                    else -> ThemeColors.onSurface
                }
                MaaUiSurface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (itemEnabled || isSelected) Modifier.clickable { onSelected(value) }
                            else Modifier
                        ),
                    color = chipColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    MaaUiText(
                        text = displayName,
                        style = ThemeTypography.bodySmall,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
