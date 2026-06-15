package com.aliothmoon.maameow.schedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberDatePickerState

import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NumberPicker

import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Switch

import com.aliothmoon.maameow.ui.component.bridge.AppAssistChip
import com.aliothmoon.maameow.ui.component.bridge.AppInputChip

import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.ui.component.tip.ExpandableTipContent
import com.aliothmoon.maameow.ui.component.tip.ExpandableTipIcon
import com.aliothmoon.maameow.schedule.model.ScheduleType
import com.aliothmoon.maameow.ui.navigation.LocalFloatingBottomBarHeight
import com.aliothmoon.maameow.utils.i18n.asString
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.net.toUri
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference

@OptIn(ExperimentalLayoutApi::class)
@Composable

fun ScheduleEditViewMiuix(
    navController: NavController,
    strategyId: String?,
    viewModel: ScheduleEditViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage.asString()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(strategyId) { viewModel.loadStrategy(strategyId) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            if (state.needBatteryOptimization || state.needExactAlarm) {
                showPermissionDialog = true
            } else {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.onDismissError()
        }
    }

    MiuixScaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MiuixTopAppBar(
                title = if (state.isNew) stringResource(R.string.schedule_edit_title_new)
                else stringResource(R.string.schedule_edit_title_edit),
                navigationIcon = {
                        MiuixIconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },

                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                        )
                    } else {
                        MiuixButton(onClick = { viewModel.onSave() }) {
                            MiuixText(stringResource(R.string.schedule_save))
                        }
                    }
                },
            color = MiuixTheme.colorScheme.surface,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding), contentPadding = PaddingValues(bottom = LocalFloatingBottomBarHeight.current + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)) {
            item { SectionHeaderMiuix(stringResource(R.string.schedule_section_basic_info)) }
            if (!state.isNew && state.strategyId != null) {
                item {
                    MiuixText(
                        text = "ID: ${state.strategyId}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            item {
                // Miuix-native title-style label. We pass label as a @Composable lambda
                // (the only overload Miuix 0.9.2 supports) rather than relying on
                // useLabelAsPlaceholder/singleLine which the AppTextField bridge
                // notes as unsupported on this version.
                MiuixTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    label = stringResource(R.string.schedule_name),
                )
            }

            item { SectionHeaderMiuix(stringResource(R.string.schedule_section_type)) }
            item {
                ScheduleTypeSelectorMiuix(
                    selected = state.scheduleType,
                    onSelect = viewModel::onScheduleTypeChanged,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            when (state.scheduleType) {
                ScheduleType.FIXED_TIME -> {
                    item { SectionHeaderMiuix(stringResource(R.string.schedule_section_days)) }
                    item {
                        // Miuix-native days-of-week dropdown. Replaces the previous
                        // inline radio + chips block. Behavior:
                        //   * summary shows the current selection
                        //   * tapping opens a dialog with two sections:
                        //       1. "Every day" radio — picking it sets all 7 days
                        //       2. 7-day multi-select via CheckboxPreference — only
                        //          enabled when "Every day" is off
                        val allSelected = DayOfWeek.entries.all { it in state.daysOfWeek }
                        val daysSummary = if (allSelected) {
                            stringResource(R.string.schedule_every_day)
                        } else if (state.daysOfWeek.isEmpty()) {
                            // No days selected. Surface the validation message
                            // directly so the user sees something concrete in the
                            // summary row (which previously fell back to "Every
                            // day" via .ifEmpty and was misleading).
                            stringResource(R.string.schedule_error_days_required)
                        } else {
                            // Resolve each day label in @Composable scope *before*
                            // joinToString: scheduleDayChipLabel is @Composable, and
                            // joinToString's transform lambda is not @Composable.
                            val dayNames = state.daysOfWeek
                                .sortedBy { it.value }
                                .map { scheduleDayChipLabel(it) }
                            dayNames.joinToString(stringResource(R.string.common_enumeration_separator))
                        }
                        var showDaysDialog by remember { mutableStateOf(false) }
                        ArrowPreference(
                            title = stringResource(R.string.schedule_section_days),
                            summary = daysSummary,
                            onClick = { showDaysDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                        if (showDaysDialog) {
                            val dialogAllSelected = DayOfWeek.entries.all { it in state.daysOfWeek }
                            OverlayDialog(
                                show = true,
                                title = stringResource(R.string.schedule_section_days),
                                onDismissRequest = { showDaysDialog = false },
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // 7-day multi-select. Sits at the top of the
                                    // dialog so the individual days are the first
                                    // thing the user sees; "Every day" below acts as
                                    // a shortcut. When "Every day" is checked, this
                                    // list is hidden — the master shortcut makes the
                                    // 7 individual rows redundant, and hiding them
                                    // prevents the user from accidentally deselecting
                                    // a day while thinking "every day" is just a label.
                                    if (!dialogAllSelected) {
                                        DayOfWeek.entries.forEach { day ->
                                            val checked = day in state.daysOfWeek
                                            CheckboxPreference(
                                                title = scheduleDayChipLabel(day),
                                                checked = checked,
                                                onCheckedChange = { newChecked ->
                                                    viewModel.onSetDays(
                                                        if (newChecked) state.daysOfWeek + day
                                                        else state.daysOfWeek - day,
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                    // "Every day" master switch. Positioned at the
                                    // bottom of the dialog (below the 7-day list
                                    // when visible) so the per-day controls are the
                                    // primary surface and "Every day" reads as a
                                    // bulk-select shortcut — checking it hides the
                                    // 7-day list above; unchecking re-exposes it.
                                    CheckboxPreference(
                                        title = stringResource(R.string.schedule_every_day),
                                        checked = dialogAllSelected,
                                        onCheckedChange = { newChecked ->
                                            if (newChecked) {
                                                // Rising edge: turn on "every day" by
                                                // selecting all 7 days.
                                                viewModel.onSetDays(DayOfWeek.entries.toSet())
                                            } else {
                                                // Falling edge: uncheck "every day" by
                                                // clearing the day set. Bidirectional —
                                                // users can wipe their selection in one
                                                // tap and then pick individual days above.
                                                viewModel.onSetDays(emptySet())
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    // Dialog buttons (cancel / confirm). Confirm is a no-op here
                                    // because the dialog commits on every click; we still show
                                    // it so the user has a clear way to dismiss.
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        MiuixButton(onClick = { showDaysDialog = false }) {
                                            MiuixText(stringResource(R.string.common_confirm))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { SectionHeaderMiuix(stringResource(R.string.schedule_section_times)) }
                    item {
                        // In-row edit sheet: when a user taps an existing HH:mm chip we
                        // capture which time is being edited and open TimePickerDialog
                        // with that as initialTime; onConfirm calls onReplaceTime(old, new)
                        // so the entry in executionTimes is updated in place.
                        var showEditTimePicker by remember { mutableStateOf(false) }
                        var editingTime by remember { mutableStateOf<LocalTime?>(null) }
                        var showAddTimePicker by remember { mutableStateOf(false) }
                        // FlowRow of chips. One InputChip per existing time (with a
                        // trailing × IconButton to remove it and an onClick that reopens
                        // the picker for editing), followed by a single AssistChip for
                        // + Add Time. Mirrors the Material UI in ScheduleEditViewMaterial.
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            state.executionTimes.forEach { time ->
                                AppInputChip(
                                    selected = false,
                                    onClick = {
                                        editingTime = time
                                        showEditTimePicker = true
                                    },
                                    label = { MiuixText("%02d:%02d".format(time.hour, time.minute)) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { viewModel.onRemoveTime(time) },
                                            modifier = Modifier.size(18.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.common_delete),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    },
                                )
                            }
                            AppAssistChip(
                                onClick = {
                                    editingTime = null
                                    showAddTimePicker = true
                                },
                                label = { MiuixText(stringResource(R.string.schedule_add_time)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                        if (showAddTimePicker) {
                            TimePickerDialog(
                                initialTime = null,
                                onDismiss = { showAddTimePicker = false },
                                onConfirm = { time ->
                                    viewModel.onAddTime(time)
                                    showAddTimePicker = false
                                },
                            )
                        }
                        if (showEditTimePicker && editingTime != null) {
                            TimePickerDialog(
                                initialTime = editingTime,
                                onDismiss = {
                                    showEditTimePicker = false
                                    editingTime = null
                                },
                                onConfirm = { newTime ->
                                    val old = editingTime
                                    if (old != null) viewModel.onReplaceTime(old, newTime)
                                    showEditTimePicker = false
                                    editingTime = null
                                },
                            )
                        }
                    }
                }

                ScheduleType.INTERVAL -> {
                    item { SectionHeaderMiuix(stringResource(R.string.schedule_section_start_time)) }
                    item {
                        var showDatePicker by remember { mutableStateOf(false) }
                        var showStartTimePicker by remember { mutableStateOf(false) }
                        var pendingDateMs by remember { mutableStateOf<Long?>(null) }
                        val displayText = state.startTimeMs?.let { ms ->
                            Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        } ?: stringResource(R.string.schedule_tap_to_choose)

                        // Miuix-native: read-only field driven by outer Modifier.clickable. The
                        // inner TextField is disabled so the platform keyboard never appears; the
                        // date+time picker dialog is the sole editing surface.
                        MiuixTextField(
                            value = displayText,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                ) { showDatePicker = true },
                            enabled = false,
                            label = stringResource(R.string.schedule_first_execution_time),
                        )

                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.startTimeMs ?: System.currentTimeMillis())
                            // Miuix 0.9.2 has no native DatePicker; fall back to Material3 inline
                            // (NOT via the bridge — keeping the call site explicit so the
                            // Miuix-style MiuixButton confirm/dismiss pair controls the dialog
                            // chrome instead of the bridge wrapper's default buttons).
                            androidx.compose.material3.DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    MiuixButton(onClick = {
                                        pendingDateMs = datePickerState.selectedDateMillis
                                        showDatePicker = false
                                        showStartTimePicker = true
                                    }) { MiuixText(stringResource(R.string.schedule_next_step)) }
                                },
                                dismissButton = {
                                    MiuixButton(onClick = { showDatePicker = false }) {
                                        MiuixText(stringResource(R.string.common_cancel))
                                    }
                                },
                            ) {
                                androidx.compose.material3.DatePicker(state = datePickerState)
                            }
                        }

                        if (showStartTimePicker) {
                            val existingTime = state.startTimeMs?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
                            TimePickerDialog(initialTime = existingTime, onDismiss = { showStartTimePicker = false }, onConfirm = { time ->
                                val dateMs = pendingDateMs ?: return@TimePickerDialog
                                val date = Instant.ofEpochMilli(dateMs).atZone(ZoneId.of("UTC")).toLocalDate()
                                val combined = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                viewModel.onStartTimeChanged(combined)
                                showStartTimePicker = false
                            })
                        }
                    }

                    item { SectionHeaderMiuix(stringResource(R.string.schedule_section_interval)) }
                    item {
                        val daysUnit = stringResource(R.string.schedule_days_unit)
                        val hoursUnit = stringResource(R.string.schedule_hours_unit)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiuixTextField(
                                value = if (state.intervalDays > 0) state.intervalDays.toString() else "",
                                onValueChange = { input: String -> viewModel.onIntervalDaysChanged(input.toIntOrNull() ?: 0) },
                                label = daysUnit,
                                modifier = Modifier.weight(1f),
                            )
                            MiuixTextField(
                                value = if (state.intervalHours > 0) state.intervalHours.toString() else "",
                                onValueChange = { input: String -> viewModel.onIntervalHoursChanged(input.toIntOrNull() ?: 0) },
                                label = hoursUnit,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        val totalMinutes = state.intervalDays * 24 * 60 + state.intervalHours * 60
                        if (totalMinutes > 0) {
                            MiuixText(
                                text = stringResource(R.string.schedule_total_hours, totalMinutes / 60),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { SectionHeaderMiuix(stringResource(R.string.schedule_section_task_config)) }
            item {
                if (state.profiles.isEmpty()) {
                    MiuixText(text = stringResource(R.string.schedule_no_profiles), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    ProfileSelectorMiuix(
                        profiles = state.profiles,
                        selectedId = state.selectedProfileId,
                        onSelect = viewModel::onSelectProfile,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    val selectedProfile = state.profiles.find { it.id == state.selectedProfileId }
                    val enabledTasks = selectedProfile?.chain?.filter { it.enabled }?.joinToString("、") { it.name }
                    if (!enabledTasks.isNullOrEmpty()) {
                        MiuixText(text = stringResource(R.string.schedule_enabled_tasks, enabledTasks), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                    }
                }
            }

            item {
                SectionHeaderMiuix(stringResource(R.string.schedule_section_advanced))
                val (expanded, setExpanded) = remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        MiuixText(stringResource(R.string.schedule_force_start), style = MiuixTheme.textStyles.body1)
                        ExpandableTipIcon(modifier = Modifier.padding(start = 8.dp), expanded = expanded, onExpandedChange = { setExpanded(it) })
                    }
                    MiuixSwitch(checked = state.forceStart, onCheckedChange = { viewModel.onForceStartChanged(it) })
                }
                ExpandableTipContent(visible = expanded, tipText = stringResource(R.string.schedule_force_start_tip), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    if (showPermissionDialog) {
        val context = LocalContext.current
        val tips = buildList {
            if (state.needBatteryOptimization) add(stringResource(R.string.schedule_permission_tip_battery_optimization))
            if (state.needExactAlarm) add(stringResource(R.string.schedule_permission_tip_exact_alarm))
        }
        OverlayDialog(
            show = true,
            title = stringResource(R.string.schedule_permission_title),
            summary = stringResource(R.string.schedule_permission_message, tips.joinToString(stringResource(R.string.common_enumeration_separator))),
            onDismissRequest = { showPermissionDialog = false; navController.popBackStack() },
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                MiuixButton(onClick = { showPermissionDialog = false; navController.popBackStack() }) { MiuixText(stringResource(R.string.schedule_later)) }
                Spacer(modifier = Modifier.width(8.dp))
                MiuixButton(onClick = {
                    if (state.needBatteryOptimization) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())) }
                    } else if (state.needExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
                    }
                    showPermissionDialog = false; navController.popBackStack()
                }) { MiuixText(stringResource(R.string.schedule_go_to_settings)) }
            }
        }
    }
}

@Composable
private fun SectionHeaderMiuix(title: String) {
    MiuixText(text = title, style = MiuixTheme.textStyles.headline2, color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
}

/**
 * Miuix-native two-segment selector for [ScheduleType]. Replaces the M3 SegmentedButton bridge
 * fallback with a hand-drawn pill row: each segment is a [MiuixSurface] (onClick) whose color
 * flips between `surface` and `secondaryContainer` based on selection. Miuix 0.9.2 has no
 * SegmentedButton, and Material3's variant clashes visually with the rest of the Miuix theme.
 */
@Composable
private fun ScheduleTypeSelectorMiuix(
    selected: ScheduleType,
    onSelect: (ScheduleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ScheduleType.entries.forEach { type ->
            val isSelected = type == selected
            MiuixSurface(
                onClick = { onSelect(type) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MiuixTheme.colorScheme.secondaryContainer
                else androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MiuixText(
                        text = when (type) {
                            ScheduleType.FIXED_TIME -> stringResource(R.string.schedule_type_fixed_time)
                            ScheduleType.INTERVAL -> stringResource(R.string.schedule_type_interval)
                        },
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Medium
                        else androidx.compose.ui.text.font.FontWeight.Normal,
                        color = if (isSelected) MiuixTheme.colorScheme.onSecondaryContainer
                        else MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * Miuix-native profile picker. Replaces the M3 FilterChip bridge fallback with a fixed grid of
 * [MiuixSurface] cells (one per profile). Each cell's background flips between `secondaryContainer`
 * (selected) and `surface` (idle) on click. Wraps to a new row when profiles overflow.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProfileSelectorMiuix(
    profiles: List<com.aliothmoon.maameow.data.model.TaskProfile>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        profiles.forEach { profile ->
            val isSelected = profile.id == selectedId
            MiuixSurface(
                onClick = { onSelect(profile.id) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MiuixTheme.colorScheme.secondaryContainer
                else MiuixTheme.colorScheme.surfaceContainer,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MiuixText(
                        text = profile.name,
                        style = MiuixTheme.textStyles.body2,
                        color = if (isSelected) MiuixTheme.colorScheme.onSecondaryContainer
                        else MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimePickerDialog(initialTime: LocalTime? = null, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    var selectedHour by remember { mutableStateOf(initialTime?.hour ?: 0) }
    var selectedMinute by remember { mutableStateOf(initialTime?.minute ?: 0) }
    // Two-mode picker: wheel (default) and keyboard. The Keyboard input button
    // toggles between them in place; Cancel / Confirm stay in the same row so
    // the user never has to look elsewhere to commit.
    var keyboardMode by remember { mutableStateOf(false) }
    // Keyboard input mode: two separate fields, one for hour and one for minute.
    // Each field is constrained to its valid range on the fly, so Confirm
    // never has to re-parse a malformed string. Switching from wheel back to
    // keyboard mode resyncs the two fields from the current wheel selection.
    // Keyboard input mode defaults to empty so the add flow shows a blank
    // field with the label acting as placeholder. The edit flow seeds the
    // values when the user flips from wheel to keyboard (see the mode
    // toggle handler below), so the initialTime path is intentionally
    // decoupled from the keyboard field state.
    var textHour by remember { mutableStateOf("") }
    var textMinute by remember { mutableStateOf("") }

    OverlayDialog(
        show = true,
        title = stringResource(R.string.schedule_time_picker_title),
        onDismissRequest = onDismiss,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            if (keyboardMode) {
                // Keyboard input mode: two centered number fields separated
                // by a literal ':'. Mirrors the wheel layout below for visual
                // consistency. Each field accepts at most two digits and is
                // coerced into its valid range on the fly, so Confirm can
                // use selectedHour / selectedMinute directly.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixTextField(
                        value = textHour,
                        onValueChange = { raw ->
                            // Keep digits only, max 2 chars, coerce to 0..23.
                            val digits = raw.filter { it.isDigit() }.take(2)
                            textHour = digits
                            digits.toIntOrNull()?.coerceIn(0, 23)?.let {
                                selectedHour = it
                            }
                        },
                        label = stringResource(R.string.schedule_time_hour_label),
                        modifier = Modifier.width(80.dp),
                    )
                    MiuixText(
                        text = ":",
                        style = MiuixTheme.textStyles.title1,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    MiuixTextField(
                        value = textMinute,
                        onValueChange = { raw ->
                            // Keep digits only, max 2 chars, coerce to 0..59.
                            val digits = raw.filter { it.isDigit() }.take(2)
                            textMinute = digits
                            digits.toIntOrNull()?.coerceIn(0, 59)?.let {
                                selectedMinute = it
                            }
                        },
                        label = stringResource(R.string.schedule_time_minute_label),
                        modifier = Modifier.width(80.dp),
                    )
                }
            } else {
                // Wheel mode: two NumberPickers centered in the dialog, separated
                // by a literal ':'.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberPicker(
                        value = selectedHour,
                        onValueChange = { selectedHour = it },
                        range = 0..23,
                        label = { "%02d".format(it) },
                        wrapAround = true,
                        modifier = Modifier.width(80.dp),
                    )
                    MiuixText(
                        text = ":",
                        style = MiuixTheme.textStyles.title1,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    NumberPicker(
                        value = selectedMinute,
                        onValueChange = { selectedMinute = it },
                        range = 0..59,
                        label = { "%02d".format(it) },
                        wrapAround = true,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }
            // Bottom action row. Layout:
            //   [Keyboard input (left)]  ...  [Cancel  Confirm (right)]
            // The left/right halves are pushed apart with SpaceBetween so the
            // user can always see both ends of the toolbar.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixButton(onClick = {
                    // Resync the text fields from the current wheel selection
                    // before flipping modes, so neither side of the round trip
                    // loses state.
                    textHour = "%02d".format(selectedHour)
                    textMinute = "%02d".format(selectedMinute)
                    keyboardMode = !keyboardMode
                }) {
                    MiuixText(stringResource(R.string.schedule_time_picker_keyboard_input))
                }
                Row {
                    MiuixButton(onClick = onDismiss) {
                        MiuixText(stringResource(R.string.common_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    MiuixButton(
                        onClick = {
                            // Both modes already keep selectedHour and
                            // selectedMinute in sync with the user's input,
                            // so Confirm can call onConfirm unconditionally.
                            onConfirm(LocalTime.of(selectedHour, selectedMinute))
                        },
                    ) {
                        MiuixText(stringResource(R.string.common_confirm))
                    }
                }
            }
        }
    }
}

/**
 * Parse an HH:mm time string. Returns null if the input is malformed or
 * out of range. Tolerant of extra whitespace; rejects 24:00, 1:00, etc.
 */
private fun parseHhMm(raw: String): LocalTime? {
    val trimmed = raw.trim()
    if (!trimmed.matches(Regex("^\\d{1,2}:\\d{2}\$"))) return null
    val (hStr, mStr) = trimmed.split(":")
    val h = hStr.toIntOrNull() ?: return null
    val m = mStr.toIntOrNull() ?: return null
    if (h !in 0..23) return null
    if (m !in 0..59) return null
    return LocalTime.of(h, m)
}
