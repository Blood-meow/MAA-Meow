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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.aliothmoon.maameow.schedule.model.ExecutionResult
import com.aliothmoon.maameow.schedule.model.TriggerLogEntry
import com.aliothmoon.maameow.schedule.service.ScheduleTriggerLogger.TriggerLogSummary
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScheduleTriggerLogViewMiuix(
    navController: NavController, viewModel: ScheduleTriggerLogViewModel
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }
    var deleteConfirmFileName by remember { mutableStateOf<String?>(null) }

    if (detail.isNotEmpty()) {
        BackHandler { viewModel.clearDetail() }
        DetailViewMiuix(
            entries = detail,
            onBack = { viewModel.clearDetail() }
        )
        return
    }

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.schedule_trigger_log_title),
                navigationIcon = {
                        MiuixIconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },

                actions = {
                    if (summaries.isNotEmpty()) {
                        MiuixIconButton(onClick = { showClearConfirm = true }) {
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
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        MiuixText(
                            stringResource(R.string.schedule_log_empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summaries, key = { it.fileName }) { summary ->
                        SummaryCardMiuix(
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
                title = { MiuixText(stringResource(R.string.schedule_log_clear_title)) },
                text = { MiuixText(stringResource(R.string.schedule_log_clear_message)) },
                confirmButton = {
                    MiuixButton(onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    }) { MiuixText(stringResource(R.string.schedule_log_clear_title)) }
                },
                dismissButton = {
                    MiuixButton(onClick = { showClearConfirm = false }) { MiuixText(stringResource(R.string.common_cancel)) }
                }
            )
        }

        if (deleteConfirmFileName != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmFileName = null },
                title = { MiuixText(stringResource(R.string.schedule_log_delete_title)) },
                text = { MiuixText(stringResource(R.string.schedule_log_delete_message)) },
                confirmButton = {
                    MiuixButton(onClick = {
                        viewModel.deleteLog(deleteConfirmFileName!!)
                        deleteConfirmFileName = null
                    }) { MiuixText(stringResource(R.string.common_delete)) }
                },
                dismissButton = {
                    MiuixButton(onClick = { deleteConfirmFileName = null }) { MiuixText(stringResource(R.string.common_cancel)) }
                }
            )
        }
    }
}

@Composable
private fun SummaryCardMiuix(
    summary: TriggerLogSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val resultColor = summary.footer?.result?.let { resultColorMiuix(it) }
        ?: MiuixTheme.colorScheme.onSurfaceVariantSummary
    val resultLabel = summary.footer?.result?.let { scheduleExecutionResultLabel(it) }
        ?: stringResource(R.string.schedule_result_in_progress)

    MiuixSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surfaceVariant,
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
                    MiuixText(
                        text = summary.header.strategyName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    MiuixText(
                        text = resultLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = resultColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    MiuixText(
                        text = stringResource(R.string.schedule_log_scheduled_time, formatTime(summary.header.scheduledTimeMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    MiuixText(
                        text = stringResource(R.string.schedule_log_triggered_time, formatTime(summary.header.actualTimeMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                if (summary.footer?.message != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    MiuixText(
                        text = summary.footer.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.8f)
                    )
                }
            }
            MiuixIconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

@Composable
private fun DetailViewMiuix(
    entries: List<TriggerLogEntry>,
    onBack: () -> Unit,
) {
    val header = entries.firstOrNull() as? TriggerLogEntry.Header

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = header?.strategyName ?: stringResource(R.string.schedule_log_detail_title),
                navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },

            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(entries, key = { index, _ -> index }) { _, entry ->
                when (entry) {
                    is TriggerLogEntry.Header -> {
                        MiuixText(
                            text = stringResource(R.string.schedule_log_scheduled_time, formatTimeFull(entry.scheduledTimeMs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        MiuixText(
                            text = stringResource(R.string.schedule_log_triggered_time, formatTimeFull(entry.actualTimeMs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    is TriggerLogEntry.Log -> {
                        Row {
                            MiuixText(
                                text = formatTimeShort(entry.time),
                                style = MaterialTheme.typography.bodySmall,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MiuixText(
                                text = entry.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }

                    is TriggerLogEntry.Footer -> {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row {
                            MiuixText(
                                text = formatTimeShort(entry.time),
                                style = MaterialTheme.typography.bodySmall,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MiuixText(
                                text = stringResource(R.string.schedule_log_result, scheduleExecutionResultLabel(entry.result)),
                                style = MaterialTheme.typography.bodySmall,
                                color = resultColorMiuix(entry.result)
                            )
                            if (entry.message != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                MiuixText(
                                    text = entry.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun resultColorMiuix(result: ExecutionResult) = when (result) {
    ExecutionResult.STARTED -> MiuixTheme.colorScheme.primary
    ExecutionResult.SKIPPED_BUSY,
    ExecutionResult.SKIPPED_LOCKED,
    ExecutionResult.CANCELLED -> MiuixTheme.colorScheme.tertiary
    ExecutionResult.FAILED_VALIDATION,
    ExecutionResult.FAILED_START,
    ExecutionResult.FAILED_UI_LAUNCH -> MiuixTheme.colorScheme.error
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