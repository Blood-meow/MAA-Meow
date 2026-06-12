package com.aliothmoon.maameow.schedule.ui

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Delete
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.schedule.model.ExecutionResult
import com.aliothmoon.maameow.schedule.service.AutoStartHelper
import com.aliothmoon.maameow.schedule.model.ScheduleStrategy
import androidx.core.content.edit
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScheduleListViewMiuix(
    navController: NavController, viewModel: ScheduleListViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    var showAutoStartGuide by remember { mutableStateOf(false) }

    LaunchedEffect(state.strategies.isNotEmpty()) {
        if (state.strategies.isNotEmpty() && AutoStartHelper.isKnownRestrictiveManufacturer()) {
            val prefs = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("autostart_guided", false)) {
                val intent = AutoStartHelper.getAutoStartIntent(context)
                if (intent != null) {
                    showAutoStartGuide = true
                    prefs.edit { putBoolean("autostart_guided", true) }
                }
            }
        }
    }

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.schedule_title),
                actions = {
                    MiuixIconButton(onClick = { navController.navigate(Routes.SCHEDULE_TRIGGER_LOG) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.schedule_trigger_log_title)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.strategies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            text = stringResource(R.string.schedule_empty_state),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        MiuixText(
                            text = stringResource(R.string.schedule_empty_hint_add),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.strategies, key = { it.id }) { strategy ->
                        val profileName = state.profiles.find { it.id == strategy.profileId }?.name
                        StrategyCardMiuix(
                            strategy = strategy,
                            profileName = profileName,
                            nextTrigger = viewModel.getNextTriggerTime(strategy),
                            onToggleEnabled = { viewModel.onToggleEnabled(strategy.id, it) },
                            onClick = { navController.navigate("schedule_edit/${strategy.id}") },
                            onDelete = { deleteConfirmId = strategy.id }
                        )
                    }
                }
            }

            // FAB placed inside content area (not scaffold's floatingActionButton)
            // so it respects outer scaffold padding from the floating bottom bar
            MiuixButton(
                onClick = { navController.navigate("schedule_edit/new") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_create_strategy))
            }

            if (deleteConfirmId != null) {
                DeleteConfirmDialogMiuix(
                    onConfirm = {
                        viewModel.onDeleteStrategy(deleteConfirmId!!)
                        deleteConfirmId = null
                    },
                    onDismiss = { deleteConfirmId = null }
                )
            }

            if (showAutoStartGuide) {
                AutoStartGuideDialogMiuix(
                    context = context,
                    onDismiss = { showAutoStartGuide = false }
                )
            }
        }
    }
}

@Composable
private fun StrategyCardMiuix(
    strategy: ScheduleStrategy,
    profileName: String?,
    nextTrigger: String?,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    MiuixSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MiuixText(
                    text = strategy.name,
                    style = MiuixTheme.textStyles.headline2,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))
                MiuixText(
                    text = localizedScheduleStrategySummary(strategy),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                if (profileName != null) {
                    MiuixText(
                        text = profileName,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }

                if (strategy.enabled && nextTrigger != null) {
                    MiuixText(
                        text = stringResource(R.string.schedule_next_trigger, nextTrigger),
                        modifier = Modifier.padding(top = 2.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary
                    )
                }

                val lastResultText = strategy.lastResult?.let {
                    formatExecutionResult(it, strategy.lastResultMessage)
                }
                if (lastResultText != null) {
                    MiuixText(
                        text = lastResultText,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = executionResultColor(
                            strategy.lastResult,
                            MiuixTheme.colorScheme.primary,
                            MiuixTheme.colorScheme.error,
                            MiuixTheme.colorScheme.secondaryVariant,
                        ),
                    )
                }
            }

            MiuixIconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            Switch(
                checked = strategy.enabled,
                onCheckedChange = onToggleEnabled
            )
        }
    }
}

@Composable
private fun DeleteConfirmDialogMiuix(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { MiuixText(text = stringResource(R.string.schedule_delete_strategy_title)) },
        text = { MiuixText(text = stringResource(R.string.schedule_delete_strategy_message)) },
        confirmButton = {
            MiuixButton(onClick = onConfirm) {
                MiuixText(stringResource(R.string.common_delete))
            }
        },
        dismissButton = {
            MiuixButton(onClick = onDismiss) {
                MiuixText(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun AutoStartGuideDialogMiuix(
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { MiuixText(text = stringResource(R.string.schedule_auto_start_permission_title)) },
        text = { MiuixText(text = stringResource(R.string.schedule_auto_start_permission_message)) },
        confirmButton = {
            MiuixButton(onClick = {
                AutoStartHelper.getAutoStartIntent(context)?.let {
                    runCatching { context.startActivity(it) }
                }
                onDismiss()
            }) {
                MiuixText(stringResource(R.string.schedule_go_to_settings))
            }
        },
        dismissButton = {
            MiuixButton(onClick = onDismiss) {
                MiuixText(stringResource(R.string.schedule_later))
            }
        }
    )
}

@Composable
private fun formatExecutionResult(result: ExecutionResult, message: String?): String {
    val label = when (result) {
        ExecutionResult.STARTED,
        ExecutionResult.FAILED_VALIDATION,
        ExecutionResult.FAILED_START,
        ExecutionResult.FAILED_UI_LAUNCH,
        ExecutionResult.SKIPPED_BUSY,
        ExecutionResult.CANCELLED -> {
            stringResource(R.string.schedule_last_result, scheduleExecutionResultLabel(result))
        }
        else -> return ""
    }
    return if (message.isNullOrBlank()) label else "$label · $message"
}

private fun executionResultColor(
    result: ExecutionResult?,
    successColor: Color,
    errorColor: Color,
    warningColor: Color,
): Color {
    return when (result) {
        ExecutionResult.STARTED -> successColor
        ExecutionResult.SKIPPED_BUSY,
        ExecutionResult.CANCELLED -> warningColor
        ExecutionResult.FAILED_VALIDATION,
        ExecutionResult.FAILED_START,
        ExecutionResult.FAILED_UI_LAUNCH -> errorColor
        else -> Color.Unspecified
    }
}
