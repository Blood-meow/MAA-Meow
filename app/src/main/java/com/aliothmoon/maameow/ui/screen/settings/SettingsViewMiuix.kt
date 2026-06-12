package com.aliothmoon.maameow.ui.screen.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.BuildConfig
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.DefaultDisplayConfig
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.data.model.update.UpdateChannel
import com.aliothmoon.maameow.domain.models.RemoteBackend
import com.aliothmoon.maameow.domain.service.LogExportService
import com.aliothmoon.maameow.domain.service.ResourceInitService
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.ui.component.dialog.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.ui.component.dialog.ReInitializeConfirmDialog
import com.aliothmoon.maameow.ui.component.dialog.ResourceInitDialog
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.theme.MaaDesignTokens
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.resolve
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsViewMiuix(
    navController: NavController,
    onViewAnnouncement: () -> Unit,
    viewModel: SettingsViewModel,
    resourceInitService: ResourceInitService,
    logExportService: LogExportService
) {
    val resourceInitState by resourceInitService.state.collectAsStateWithLifecycle()
    val debugMode by viewModel.debugMode.collectAsStateWithLifecycle()
    val autoCheckUpdate by viewModel.autoCheckUpdate.collectAsStateWithLifecycle()
    val autoDownloadUpdate by viewModel.autoDownloadUpdate.collectAsStateWithLifecycle()
    val startupBackend by viewModel.startupBackend.collectAsStateWithLifecycle()
    val skipShizukuCheck by viewModel.skipShizukuCheck.collectAsStateWithLifecycle()
    val deploymentWithPause by viewModel.deploymentWithPause.collectAsStateWithLifecycle()
    val forceFullscreenOnVirtualDisplay by viewModel.forceFullscreenOnVirtualDisplay.collectAsStateWithLifecycle()
    val allowForegroundScheduledTask by viewModel.allowForegroundScheduledTask.collectAsStateWithLifecycle()
    val tasksOverrideEnabled by viewModel.tasksOverrideEnabled.collectAsStateWithLifecycle()
    val updateChannel by viewModel.updateChannel.collectAsStateWithLifecycle()
    val backgroundResolution by viewModel.backgroundResolution.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val showRestartDialog by viewModel.showRestartDialog.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openOutputStream(uri)?.let { viewModel.exportConfig(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.let { viewModel.importConfig(it) }
    }

    backupMessage?.let { msg ->
        Toast.makeText(context, msg.resolve(context), Toast.LENGTH_SHORT).show()
        viewModel.clearBackupMessage()
    }

    var showReInitConfirm by remember { mutableStateOf(false) }
    var showDebugModeConfirm by remember { mutableStateOf(false) }

    if (showRestartDialog) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_import_success_title),
            message = stringResource(R.string.dialog_import_success_message),
            icon = Icons.Rounded.Build,
            confirmText = stringResource(R.string.common_restart_now),
            dismissText = stringResource(R.string.common_restart_later),
            onConfirm = { viewModel.confirmRestart() },
            onDismissRequest = { viewModel.dismissRestartDialog() }
        )
    }

    if (showReInitConfirm) {
        ReInitializeConfirmDialog(
            onConfirm = {
                showReInitConfirm = false
                coroutineScope.launch {
                    resourceInitService.reInitialize()
                }
            },
            onDismiss = { showReInitConfirm = false }
        )
    }

    if (showDebugModeConfirm) {
        AdaptiveTaskPromptDialog(
            visible = true,
            title = stringResource(R.string.dialog_enable_debug_title),
            message = stringResource(R.string.dialog_enable_debug_message),
            onConfirm = {
                showDebugModeConfirm = false
                viewModel.setDebugMode(true)
            },
            onDismissRequest = { showDebugModeConfirm = false },
            confirmText = stringResource(R.string.common_confirm_restart),
            dismissText = stringResource(R.string.common_cancel),
            icon = Icons.Rounded.Build
        )
    }

    if (resourceInitState is ResourceInitState.Extracting) {
        ResourceInitDialog(state = resourceInitState, onRetry = {})
    }

    val scrollBehavior = MiuixScrollBehavior()

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_title),
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 更新管理
            item {
                SettingsCardMiuixSettings(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    sectionTitle = stringResource(R.string.settings_section_update),
                    rows = listOf(
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_reinit_resource_title), summary = stringResource(R.string.settings_reinit_resource_desc), onClick = { showReInitConfirm = true }) },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_auto_check_update_title), summary = stringResource(R.string.settings_auto_check_update_desc), checked = autoCheckUpdate, onCheckedChange = { viewModel.setAutoCheckUpdate(it) }) },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_auto_download_update_title), summary = stringResource(R.string.settings_auto_download_update_desc), checked = autoDownloadUpdate, enabled = autoCheckUpdate, onCheckedChange = { viewModel.setAutoDownloadUpdate(it) }) },
                        { SettingChannelRowMiuix(selectedChannel = updateChannel, onChannelSelected = { viewModel.setUpdateChannel(it) }) }
                    )
                )
            }

            // 日志
            item {
                SettingsCardMiuixSettings(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    sectionTitle = stringResource(R.string.settings_section_log),
                    rows = listOf(
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_log_history_title), summary = stringResource(R.string.settings_log_history_desc), onClick = { navController.navigate("log_history") }) },
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_log_error_title), summary = stringResource(R.string.settings_log_error_desc), onClick = { navController.navigate("error_log") }) },
                        {
                            val logExportChooserTitle = stringResource(R.string.settings_log_export_chooser_title)
                            SettingsClickRowMiuix(title = stringResource(R.string.settings_log_export_title), summary = stringResource(R.string.settings_log_export_desc), onClick = {
                                coroutineScope.launch {
                                    logExportService.exportAllLogs()?.let { context.startActivity(Intent.createChooser(it, logExportChooserTitle)) }
                                }
                            })
                        },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_debug_mode_title), summary = stringResource(R.string.settings_debug_mode_desc), checked = debugMode, onCheckedChange = { if (it) showDebugModeConfirm = true else viewModel.setDebugMode(false) }) }
                    )
                )
            }

            // 其他设置
            item {
                SettingsCardMiuixSettings(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    sectionTitle = stringResource(R.string.settings_section_other),
                    rows = listOf(
                        { SettingRemoteBackendRowMiuix(selectedBackend = startupBackend, onBackendSelected = { viewModel.setStartupBackend(it) }) },
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_theme_settings_title), summary = stringResource(R.string.settings_theme_settings_desc), onClick = { navController.navigate(Routes.THEME_SETTINGS) }) },
                        { SettingsClickRowMiuix(title = stringResource(R.string.notification_settings_title), summary = stringResource(R.string.settings_notification_desc), onClick = { navController.navigate(Routes.NOTIFICATION) }) },
                        { SettingBackgroundResolutionRowMiuix(selectedPreference = backgroundResolution, onPreferenceSelected = { viewModel.setBackgroundResolution(it) }) },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_skip_shizuku_check), checked = skipShizukuCheck, enabled = startupBackend == RemoteBackend.SHIZUKU, onCheckedChange = { viewModel.setSkipShizukuCheck(it) }) },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_deployment_with_pause), summary = stringResource(R.string.settings_deployment_with_pause_tip), checked = deploymentWithPause, onCheckedChange = { viewModel.setDeploymentWithPause(it) }) },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_force_fullscreen_on_virtual_display), checked = forceFullscreenOnVirtualDisplay, onCheckedChange = { viewModel.setForceFullscreenOnVirtualDisplay(it) }) },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_allow_foreground_scheduled_task), checked = allowForegroundScheduledTask, onCheckedChange = { viewModel.setAllowForegroundScheduledTask(it) }) },
                        { SettingsSwitchRowMiuixSettings(title = stringResource(R.string.settings_tasks_override_title), summary = stringResource(R.string.settings_tasks_override_desc), checked = tasksOverrideEnabled, onCheckedChange = { viewModel.setTasksOverrideEnabled(it) }) },
                        {
                            AnimatedVisibility(visible = tasksOverrideEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                                SettingsClickRowMiuix(title = stringResource(R.string.settings_tasks_override_edit_title), onClick = { navController.navigate(Routes.TASK_OVERRIDE_EDITOR) })
                            }
                        }
                    )
                )
            }

            // 数据管理
            item {
                SettingsCardMiuixSettings(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    sectionTitle = stringResource(R.string.settings_section_data),
                    rows = listOf(
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_export_config_title), summary = stringResource(R.string.settings_export_config_desc), onClick = { exportLauncher.launch("maameow_config.json") }) },
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_import_config_title), summary = stringResource(R.string.settings_import_config_desc), onClick = { importLauncher.launch(arrayOf("application/json")) }) }
                    )
                )
            }

            // 关于
            item {
                SettingsCardMiuixSettings(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    sectionTitle = stringResource(R.string.settings_section_about),
                    rows = listOf(
                        { SettingsInfoRowMiuix(label = stringResource(R.string.settings_about_version), value = BuildConfig.VERSION_NAME) },
                        { SettingsInfoRowMiuix(label = stringResource(R.string.settings_about_developer), value = "Aliothmoon") },
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_about_qq_group_title), summary = stringResource(R.string.settings_about_qq_group_desc), onClick = { Misc.openUriSafely(context, "https://qm.qq.com/q/j4CFbeDQXu") }) },
                        { SettingsClickRowMiuix(title = stringResource(R.string.settings_about_announcement), onClick = { onViewAnnouncement() }) },
                        {
                            MiuixText(
                                text = stringResource(R.string.settings_about_star),
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth().clickable { Misc.openUriSafely(context, "https://github.com/Aliothmoon/MAA-Meow") }.padding(vertical = MaaDesignTokens.Spacing.listItemVertical),
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsCardMiuixSettings(
    modifier: Modifier = Modifier,
    sectionTitle: String,
    rows: List<@Composable () -> Unit>
) {
    Column(modifier = modifier) {
        MiuixText(
            text = sectionTitle,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )
        MiuixSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEachIndexed { index, row ->
                    row()
                    if (index != rows.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp),
                            thickness = MaaDesignTokens.Separator.thickness,
                            color = MiuixTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsClickRowMiuix(
    title: String,
    summary: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(text = title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
            if (summary.isNotEmpty()) {
                MiuixText(text = summary, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
        }
    }
}

@Composable
private fun SettingsSwitchRowMiuixSettings(
    title: String,
    summary: String = "",
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(text = title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.6f))
            if (summary.isNotEmpty()) {
                MiuixText(text = summary, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = if (enabled) 1f else 0.4f))
            }
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsInfoRowMiuix(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixText(text = label, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
        MiuixText(text = value, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurfaceSecondary)
    }
}

@Composable
private fun SettingChannelRowMiuix(
    selectedChannel: UpdateChannel,
    onChannelSelected: (UpdateChannel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(text = stringResource(R.string.settings_update_channel_title), style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
            MiuixText(text = stringResource(R.string.settings_update_channel_desc), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val channels = UpdateChannel.entries
            channels.forEach { channel ->
                val label = stringResource(channel.resId)
                val selected = channel == selectedChannel
                MiuixSurface(
                    modifier = Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).clickable { onChannelSelected(channel) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant,
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (selected) { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.primary); Spacer(modifier = Modifier.width(4.dp)) }
                        MiuixText(text = label, style = MiuixTheme.textStyles.body2, maxLines = 1, color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRemoteBackendRowMiuix(
    selectedBackend: RemoteBackend,
    onBackendSelected: (RemoteBackend) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(text = stringResource(R.string.settings_startup_backend_title), style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
            MiuixText(text = stringResource(R.string.settings_startup_backend_desc), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val backends = RemoteBackend.entries
            backends.forEach { backend ->
                val label = backend.display
                val selected = backend == selectedBackend
                MiuixSurface(
                    modifier = Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).clickable { onBackendSelected(backend) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant,
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (selected) { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.primary); Spacer(modifier = Modifier.width(4.dp)) }
                        MiuixText(text = label, style = MiuixTheme.textStyles.body2, maxLines = 1, color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingBackgroundResolutionRowMiuix(
    selectedPreference: DefaultDisplayConfig.ResolutionPreference,
    onPreferenceSelected: (DefaultDisplayConfig.ResolutionPreference) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(text = stringResource(R.string.settings_background_resolution_title), style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
            MiuixText(text = stringResource(R.string.settings_background_resolution_desc), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf(DefaultDisplayConfig.ResolutionPreference.P720 to "720p", DefaultDisplayConfig.ResolutionPreference.P1080 to "1080p")
            options.forEach { (value, label) ->
                val selected = value == selectedPreference
                MiuixSurface(
                    modifier = Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).clickable { onPreferenceSelected(value) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant,
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (selected) { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MiuixTheme.colorScheme.primary); Spacer(modifier = Modifier.width(4.dp)) }
                        MiuixText(text = label, style = MiuixTheme.textStyles.body2, maxLines = 1, color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary)
                    }
                }
            }
        }
    }
}