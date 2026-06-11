package com.aliothmoon.maameow.ui.screen.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aliothmoon.maameow.ui.component.dialog.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.ui.viewmodel.ErrorLogViewModel
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
fun ErrorLogViewMiuix(
    navController: NavController,
    viewModel: ErrorLogViewModel = koinViewModel()
) {
    val logFiles by viewModel.logFiles.collectAsStateWithLifecycle()
    val selectedContent by viewModel.selectedContent.collectAsStateWithLifecycle()
    val selectedFileName by viewModel.selectedFileName.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val exportIntent by viewModel.exportIntent.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val exportChooserTitle = stringResource(R.string.settings_log_export_chooser_title)
    LaunchedEffect(exportIntent) {
        exportIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, exportChooserTitle))
            viewModel.clearExportIntent()
        }
    }

    BackHandler(enabled = selectedContent != null) { viewModel.clearSelectedLog() }

    if (selectedContent != null) {
        ErrorLogDetailMiuix(
            content = selectedContent!!,
            onBack = { viewModel.clearSelectedLog() }
        )
    } else {
        ErrorLogListMiuix(
            logFiles = logFiles,
            isLoading = isLoading,
            onFileClick = { viewModel.loadLogContent(it) },
            onCleanup = { viewModel.cleanupAll() },
            onExport = { viewModel.exportLogs() },
            onBack = { navController.navigateUp() }
        )
    }
}

@Composable
private fun ErrorLogListMiuix(
    logFiles: List<ErrorLogViewModel.ErrorLogFile>,
    isLoading: Boolean,
    onFileClick: (ErrorLogViewModel.ErrorLogFile) -> Unit,
    onCleanup: () -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    var showCleanupConfirm by remember { mutableStateOf(false) }

    if (showCleanupConfirm) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_clear_error_log_title),
            message = stringResource(R.string.dialog_clear_error_log_message),
            onConfirm = { onCleanup(); showCleanupConfirm = false },
            onDismissRequest = { showCleanupConfirm = false },
            confirmText = stringResource(R.string.log_cleanup_all),
            dismissText = stringResource(R.string.common_cancel),
            icon = Icons.Rounded.Delete,
            confirmColor = MiuixTheme.colorScheme.error
        )
    }

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_log_error_title),
                navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },

                actions = {
                    MiuixIconButton(onClick = onExport) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.common_export), tint = MiuixTheme.colorScheme.primary)
                    }
                    MiuixButton(onClick = { showCleanupConfirm = true }) {
                        MiuixText(stringResource(R.string.log_cleanup_all))
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
                    text = stringResource(R.string.log_empty_error),
                    modifier = Modifier.align(Alignment.Center),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = logFiles, key = { it.name }) { logFile ->
                        MiuixSurface(
                            modifier = Modifier.fillMaxWidth().clickable { onFileClick(logFile) },
                            shape = RoundedCornerShape(18.dp),
                            color = MiuixTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = logFile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MiuixText(text = formatFileSize(logFile.size), style = MaterialTheme.typography.bodySmall, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                        MiuixText(text = "•", style = MaterialTheme.typography.bodySmall, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                        MiuixText(text = formatTime(logFile.lastModified), style = MaterialTheme.typography.bodySmall, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorLogDetailMiuix(
    content: String,
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
        val lines = remember(content) { content.lines() }
        val horizontalScrollState = rememberScrollState()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).horizontalScroll(horizontalScrollState),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            itemsIndexed(items = lines) { _, line ->
                Text(
                    text = line,
                    color = getErrorLogLineColor(line),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    softWrap = false
                )
            }
        }
    }
}

private fun getErrorLogLineColor(line: String): Color = when {
    line.contains("[ERROR]") -> Color(0xFFF44336)
    line.contains("[WARN]") -> Color(0xFFFF9800)
    line.contains("[ASSERT]") -> Color(0xFFB71C1C)
    else -> Color.Unspecified
}

private fun formatTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (Z)"))

private fun formatFileSize(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    else -> "${size / (1024 * 1024)} MB"
}