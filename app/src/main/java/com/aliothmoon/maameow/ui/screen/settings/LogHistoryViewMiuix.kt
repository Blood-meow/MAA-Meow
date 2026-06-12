package com.aliothmoon.maameow.ui.screen.settings

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Delete
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon

import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.log.LogEntry
import com.aliothmoon.maameow.data.log.LogFileInfo
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.ui.component.dialog.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.ui.viewmodel.LogHistoryViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
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
fun LogHistoryViewMiuix(
    navController: NavController,
    viewModel: LogHistoryViewModel = koinViewModel(),
    logExportService: LogExportService = koinInject()
) {
    val logFiles by viewModel.logFiles.collectAsStateWithLifecycle()
    val selectedLogEntries by viewModel.selectedLogEntries.collectAsStateWithLifecycle()
    val selectedFileName by viewModel.selectedFileName.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportChooserTitle = stringResource(R.string.settings_log_export_chooser_title)

    BackHandler(enabled = selectedLogEntries != null) { viewModel.clearSelectedLog() }

    if (selectedLogEntries != null) {
        LogDetailMiuix(
            entries = selectedLogEntries!!,
            onBack = { viewModel.clearSelectedLog() }
        )
    } else {
        LogFileListMiuix(
            logFiles = logFiles,
            isLoading = isLoading,
            onFileClick = { viewModel.loadLogContent(it) },
            onFileDelete = { viewModel.deleteLogFile(it) },
            onCleanup = { viewModel.cleanupOldLogs() },
            onExport = {
                coroutineScope.launch {
                    val intent = logExportService.exportAllLogs()
                    if (intent != null) {
                        context.startActivity(Intent.createChooser(intent, exportChooserTitle))
                    }
                }
            },
            onBack = { navController.navigateUp() }
        )
    }
}

@Composable
private fun LogFileListMiuix(
    logFiles: List<LogFileInfo>,
    isLoading: Boolean,
    onFileClick: (LogFileInfo) -> Unit,
    onFileDelete: (LogFileInfo) -> Unit,
    onCleanup: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<LogFileInfo?>(null) }

    showDeleteConfirm?.let { logFile ->
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_delete_log_title),
            message = stringResource(R.string.dialog_delete_log_message, logFile.displayTime),
            onConfirm = { onFileDelete(logFile); showDeleteConfirm = null },
            onDismissRequest = { showDeleteConfirm = null },
            confirmText = stringResource(R.string.common_delete),
            dismissText = stringResource(R.string.common_cancel),
            icon = Icons.Rounded.Delete,
            confirmColor = MiuixTheme.colorScheme.error
        )
    }

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_log_history_title),
                navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },

                actions = {
                    MiuixIconButton(onClick = onExport) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.common_export), tint = MiuixTheme.colorScheme.primary)
                    }
                    MiuixButton(onClick = onCleanup) {
                        MiuixText(stringResource(R.string.log_cleanup_30_days))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && logFiles.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (logFiles.isEmpty()) {
                MiuixText(
                    text = stringResource(R.string.log_empty_history),
                    modifier = Modifier.align(Alignment.Center),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = logFiles, key = { it.fileName }) { logFile ->
                        LogFileItemMiuix(
                            logFile = logFile,
                            onClick = { onFileClick(logFile) },
                            onDelete = { showDeleteConfirm = logFile }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFileItemMiuix(
    logFile: LogFileInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    MiuixSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MiuixText(
                    text = logFile.displayTime,
                    style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Medium),
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                MiuixText(
                    text = stringResource(R.string.log_list_meta, logFile.taskCount, formatFileSizeMiuix(logFile.fileSize)),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
            MiuixIconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = MiuixTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LogDetailMiuix(
    entries: List<LogEntry>,
    onBack: () -> Unit
) {
    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.log_detail_title),
                navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            itemsIndexed(
                items = entries,
                key = { index, entry ->
                    when (entry) {
                        is LogEntry.Header -> "header_${index}_${entry.startTime}"
                        is LogEntry.Log -> "log_${index}"
                        is LogEntry.Footer -> "footer_${index}_${entry.endTime}"
                    }
                }
            ) { _, entry ->
                when (entry) {
                    is LogEntry.Header -> {
                        Column {
                            Text(text = stringResource(R.string.log_detail_task_start), color = MiuixTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Text(text = stringResource(R.string.log_detail_time, formatTimeMiuix(entry.startTime)), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Text(text = stringResource(R.string.log_detail_tasks, entry.tasks.joinToString(", ")), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Text(text = "==============================", color = MiuixTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    is LogEntry.Log -> {
                        Text(
                            text = "[${formatTimeShortMiuix(entry.time)}] [${entry.level}] ${entry.content}",
                            color = getLogLevelColorMiuix(entry.level),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                    is LogEntry.Footer -> {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = stringResource(R.string.log_detail_task_end), color = MiuixTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Text(text = stringResource(R.string.log_detail_end_time, formatTimeMiuix(entry.endTime)), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Text(
                                text = stringResource(R.string.log_detail_status, entry.status),
                                color = if (entry.status == "COMPLETED") Color(0xFF4CAF50) else Color(0xFFF44336),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(text = "==============================", color = MiuixTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun getLogLevelColorMiuix(level: String): Color = when (level) {
    "ERROR" -> Color(0xFFF44336)
    "WARNING" -> Color(0xFFFF9800)
    "SUCCESS" -> Color(0xFF4CAF50)
    "INFO" -> Color(0xFF2196F3)
    "RARE" -> Color(0xFFE040FB)
    "TRACE" -> Color(0xFFC0C4CC)
    "MESSAGE" -> Color(0xFF909399)
    else -> Color(0xFF333333)
}

private fun formatTimeMiuix(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (Z)"))

private fun formatTimeShortMiuix(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

private fun formatFileSizeMiuix(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    else -> "${size / (1024 * 1024)} MB"
}
