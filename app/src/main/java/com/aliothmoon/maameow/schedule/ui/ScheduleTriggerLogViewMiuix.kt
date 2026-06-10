package com.aliothmoon.maameow.schedule.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Delete
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.basic.AlertDialog
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Divider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.schedule.model.ExecutionResult
import com.aliothmoon.maameow.schedule.model.TriggerLogEntry
import com.aliothmoon.maameow.schedule.service.ScheduleTriggerLogger.TriggerLogSummary
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleTriggerLogViewMaterial(
    navController: NavController,
    viewModel: ScheduleTriggerLogViewModel = koinViewModel(),
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }
    var deleteConfirmFileName by remember { mutableStateOf<String?>(null) }

    // 详情模式
    if (detail.isNotEmpty()) {
        BackHandler { viewModel.clearDetail() }
        DetailView(
            entries = detail,
            onBack = { viewModel.clearDetail() }
        )
        return
    }

    // 列表模式
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.schedule_trigger_log_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = { navController.popBackStack() },
                actions = {
                    if (summaries.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.schedule_log_clear_title))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            summaries.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.schedule_log_empty_state),
                            style = MiuixTheme.typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    insideMargin = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summaries, key = { it.fileName }) { summary ->
                        SummaryCard(
                            summary = summary,
                            onClick = { viewModel.loadDetail(summary.fileName) },
                            onDelete = { deleteConfirmFileName = summary.fileName }
                        )
                    }
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(stringResource(R.string.schedule_log_clear_title)) },
                text = { Text(stringResource(R.string.schedule_log_clear_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    }) { Text(stringResource(R.string.schedule_log_clear_title), color = colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (deleteConfirmFileName != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmFileName = null },
                title = { Text(stringResource(R.string.schedule_log_delete_title)) },
                text = { Text(stringResource(R.string.schedule_log_delete_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteLog(deleteConfirmFileName!!)
                        deleteConfirmFileName = null
                    }) { Text(stringResource(R.string.common_delete), color = colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirmFileName = null }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }
    }
}

// ==================== 列表卡片 ====================

@Composable
private fun SummaryCard(
    summary: TriggerLogSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val resultColor = summary.footer?.result?.let { resultColor(it) }
        ?: colorScheme.onSurfaceVariant
    val resultLabel = summary.footer?.result?.let { scheduleExecutionResultLabel(it) }
        ?: stringResource(R.string.schedule_result_in_progress)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.header.strategyName,
                        style = MiuixTheme.typography.titleSmall
                    )
                    Text(
                        text = resultLabel,
                        style = MiuixTheme.typography.labelMedium,
                        color = resultColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = stringResource(
                            R.string.schedule_log_scheduled_time,
                            formatTime(summary.header.scheduledTimeMs)
                        ),
                        style = MiuixTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(
                            R.string.schedule_log_triggered_time,
                            formatTime(summary.header.actualTimeMs)
                        ),
                        style = MiuixTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                if (summary.footer?.message != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary.footer.message,
                        style = MiuixTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== 详情视图 ====================

@Composable
private fun DetailView(
    entries: List<TriggerLogEntry>,
    onBack: () -> Unit,
) {
    val header = entries.firstOrNull() as? TriggerLogEntry.Header

    Scaffold(
        topBar = {
            TopAppBar(
                title = header?.strategyName ?: stringResource(R.string.schedule_log_detail_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            insideMargin = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(entries, key = { index, _ -> index }) { _, entry ->
                when (entry) {
                    is TriggerLogEntry.Header -> {
                        Text(
                            text = stringResource(
                                R.string.schedule_log_scheduled_time,
                                formatTimeFull(entry.scheduledTimeMs)
                            ),
                            style = MiuixTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                R.string.schedule_log_triggered_time,
                                formatTimeFull(entry.actualTimeMs)
                            ),
                            style = MiuixTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    is TriggerLogEntry.Log -> {
                        Row {
                            Text(
                                text = formatTimeShort(entry.time),
                                style = MiuixTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.message,
                                style = MiuixTheme.typography.bodySmall,
                            )
                        }
                    }

                    is TriggerLogEntry.Footer -> {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row {
                            Text(
                                text = formatTimeShort(entry.time),
                                style = MiuixTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.schedule_log_result,
                                    scheduleExecutionResultLabel(entry.result)
                                ),
                                style = MiuixTheme.typography.bodySmall,
                                color = resultColor(entry.result)
                            )
                            if (entry.message != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.message,
                                    style = MiuixTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 工具方法 ====================

@Composable
private fun resultColor(result: ExecutionResult) = when (result) {
    ExecutionResult.STARTED -> colorScheme.primary
    ExecutionResult.SKIPPED_BUSY,
    ExecutionResult.SKIPPED_LOCKED,
    ExecutionResult.CANCELLED -> colorScheme.tertiary

    ExecutionResult.FAILED_VALIDATION,
    ExecutionResult.FAILED_START,
    ExecutionResult.FAILED_UI_LAUNCH -> colorScheme.error
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

private fun formatTime(epochMs: Long): String {
    if (epochMs <= 0) return "--"
    return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
}

private fun formatTimeFull(epochMs: Long): String {
    if (epochMs <= 0) return "--"
    return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(fullDateTimeFormatter)
}

private fun formatTimeShort(epochMs: Long): String {
    if (epochMs <= 0) return "--"
    return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(shortTimeFormatter)
}
