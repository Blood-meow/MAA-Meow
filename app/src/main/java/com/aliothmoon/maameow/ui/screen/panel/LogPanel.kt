package com.aliothmoon.maameow.ui.screen.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import com.aliothmoon.maameow.ui.component.material.MaaUiIconButton
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.LogItem
import com.aliothmoon.maameow.ui.component.dialog.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

/**
 * 日志面板
 * 以浮层形式显示任务执行日志
 */
@Composable
fun LogPanel(
    logs: List<LogItem>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    var isAutoScroll by remember { mutableStateOf(true) }
    var selectedLog by remember { mutableStateOf<LogItem?>(null) }

    // 自动滚动到最新日志
    LaunchedEffect(logs.size) {
        if (isAutoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    // 检测用户手动滚动以停止自动滚动
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isAutoScroll = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部工具栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.List,
                    contentDescription = null,
                    tint = ThemeColors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                MaaUiText(
                    text = stringResource(R.string.panel_log_title),
                    style = ThemeTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                MaaUiIconButton(onClick = onClearLogs) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.common_clear),
                        tint = ThemeColors.onSurfaceVariant
                    )
                }
                MaaUiIconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = ThemeColors.onSurfaceVariant
                    )
                }
            }
        }

        // 日志列表
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = logs,
                    key = { it.id }
                ) { logItem ->
                    LogLine(
                        logItem = logItem,
                        onClick = { selectedLog = logItem }
                    )
                }
            }

            // 自动滚动恢复按钮
            if (!isAutoScroll && logs.isNotEmpty()) {
                MaaUiIconButton(
                    onClick = { isAutoScroll = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(
                            ThemeColors.primaryContainer,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.panel_log_resume_auto_scroll),
                        tint = ThemeColors.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // 日志详情弹窗
    selectedLog?.let { log ->
        LogDetailDialog(
            logItem = log,
            onDismiss = { selectedLog = null }
        )
    }
}

/**
 * 单条日志行
 */
@Composable
private fun LogLine(
    logItem: LogItem,
    onClick: () -> Unit
) {
    MaaUiSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 时间戳
            MaaUiText(
                text = logItem.formattedTime,
                style = ThemeTypography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = ThemeColors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.width(55.dp)
            )

            // 级别标识
            MaaUiSurface(
                shape = RoundedCornerShape(3.dp),
                color = logItem.color.copy(alpha = 0.15f)
            ) {
                MaaUiText(
                    text = logItem.level.displayName,
                    style = ThemeTypography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = logItem.color,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 日志内容
            MaaUiText(
                text = logItem.content,
                style = ThemeTypography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                color = logItem.color,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // 详情图标
            if (logItem.hasDetails) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.panel_log_view_details),
                    modifier = Modifier.size(14.dp),
                    tint = ThemeColors.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 日志详情弹窗
 */
@Composable
private fun LogDetailDialog(
    logItem: LogItem,
    onDismiss: () -> Unit
) {
    AdaptiveTaskPromptDialog(
        visible = true,
        title = stringResource(R.string.log_detail_title),
        onConfirm = onDismiss,
        onDismissRequest = onDismiss,
        confirmText = stringResource(R.string.common_confirm),
        dismissText = null,
        icon = Icons.Rounded.Info,
        iconTint = logItem.color,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaaUiSurface(
                        shape = RoundedCornerShape(4.dp),
                        color = logItem.color.copy(alpha = 0.15f)
                    ) {
                        MaaUiText(
                            text = logItem.level.displayName,
                            style = ThemeTypography.labelMedium,
                            color = logItem.color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    MaaUiText(
                        text = logItem.formattedTime,
                        style = ThemeTypography.bodySmall,
                        color = ThemeColors.onSurfaceVariant
                    )
                }

                // 日志内容
                MaaUiText(
                    text = logItem.content,
                    style = ThemeTypography.bodyMedium,
                    color = logItem.color
                )

                // 详细信息 (tooltip)
                val richTooltip = logItem.annotatedTooltip
                val plainTooltip = logItem.tooltip
                if (richTooltip != null || plainTooltip != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MaaUiText(
                            text = stringResource(R.string.panel_log_details_section),
                            style = ThemeTypography.labelMedium,
                            color = ThemeColors.onSurfaceVariant
                        )
                        MaaUiSurface(
                            shape = RoundedCornerShape(4.dp),
                            color = ThemeColors.surfaceVariant
                        ) {
                            if (richTooltip != null) {
                                MaaUiText(
                                    text = richTooltip,
                                    style = ThemeTypography.bodySmall,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            } else {
                                MaaUiText(
                                    text = plainTooltip!!,
                                    style = ThemeTypography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = ThemeColors.onSurface,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }

                // 截图路径（预留）
                logItem.screenshotPath?.let { path ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MaaUiText(
                            text = stringResource(R.string.panel_log_screenshot_section),
                            style = ThemeTypography.labelMedium,
                            color = ThemeColors.onSurfaceVariant
                        )
                        MaaUiText(
                            text = path,
                            style = ThemeTypography.bodySmall,
                            color = ThemeColors.primary,
                            modifier = Modifier.clickable { /* TODO: 打开图片 */ }
                        )
                    }
                }
            }
        }
    )
}
