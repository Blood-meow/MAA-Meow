package com.aliothmoon.maameow.ui.component.tip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

/**
 * 可展开的提示文字区域
 * 配合 ExpandableTipIcon 使用，显示展开后的提示内容
 */
@Composable
fun ExpandableTipContent(
    visible: Boolean,
    tipText: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = ThemeColors.secondaryContainer,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = tipText,
                style = ThemeTypography.bodySmall,
                color = ThemeColors.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}