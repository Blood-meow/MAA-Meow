package com.aliothmoon.maameow.ui.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

/**
 * 资源初始化弹窗
 * 显示初始化进度或失败信息
 */
@Composable
fun ResourceInitDialog(
    state: ResourceInitState,
    onDismiss: () -> Unit = {},
    onRetry: () -> Unit = {}
) {

    when (state) {
        is ResourceInitState.Extracting -> {
            // 解压进度弹窗（不可关闭）
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                MaaUiSurface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThemeColors.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MaaUiText(
                            text = stringResource(R.string.resource_init_in_progress_title),
                            style = ThemeTypography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 进度条
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 进度文本
                        MaaUiText(
                            text = "${state.extractedCount} / ${state.totalCount} (${state.progress}%)",
                            style = ThemeTypography.bodyMedium,
                            color = ThemeColors.onSurfaceVariant
                        )

                        if (state.currentFile.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            MaaUiText(
                                text = state.currentFile,
                                style = ThemeTypography.bodySmall,
                                color = ThemeColors.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        MaaUiText(
                            text = stringResource(R.string.resource_init_in_progress_message),
                            style = ThemeTypography.bodySmall,
                            color = ThemeColors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        is ResourceInitState.Failed -> {
            // 失败弹窗
            AdaptiveTaskPromptDialog(
                visible = true,
                title = stringResource(R.string.resource_init_failed_title),
                message = stringResource(R.string.resource_init_failed_message, state.message),
                onConfirm = onRetry,
                onDismissRequest = onDismiss,
                confirmText = stringResource(R.string.resource_init_retry),
                dismissText = stringResource(R.string.common_cancel),
                icon = Icons.Rounded.Warning,
                confirmColor = ThemeColors.error
            )
        }

        else -> {
            // 其他状态不显示弹窗
        }
    }
}

/**
 * 重新初始化确认弹窗
 */
@Composable
fun ReInitializeConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AdaptiveTaskPromptDialog(
        visible = true,
        title = stringResource(R.string.resource_init_reinitialize_title),
        message = stringResource(R.string.resource_init_reinitialize_message),
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.resource_init_reinitialize_confirm),
        dismissText = stringResource(R.string.common_cancel),
        icon = Icons.Rounded.Refresh,
        confirmColor = ThemeColors.error
    )
}
