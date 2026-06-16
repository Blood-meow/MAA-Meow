package com.aliothmoon.maameow.ui.screen.panel.mall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aliothmoon.maameow.ui.component.material.MaaUiButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import com.aliothmoon.maameow.ui.component.material.MaaUiTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.ui.component.ITextField
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

/**
 * 添加黑名单面板 - 输入框形式
 */
@Composable
fun InlineBlacklistAddPanel(
    onItemAdded: (String) -> Unit,
    onCancel: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    MaaUiSurface(
        modifier = Modifier.fillMaxWidth(),
        color = ThemeColors.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MaaUiText(
                stringResource(R.string.panel_mall_add_blacklist_title),
                style = ThemeTypography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            ITextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.panel_mall_add_blacklist_placeholder),
                singleLine = true,
                onImeAction = {
                    if (inputText.isNotBlank()) {
                        onItemAdded(inputText)
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaaUiTextButton(onClick = onCancel) { MaaUiText(stringResource(R.string.common_cancel)) }
                Spacer(modifier = Modifier.width(8.dp))
                MaaUiButton(
                    onClick = { onItemAdded(inputText) },
                    enabled = inputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.error)
                ) { MaaUiText(stringResource(R.string.common_add)) }
            }
        }
    }
}
