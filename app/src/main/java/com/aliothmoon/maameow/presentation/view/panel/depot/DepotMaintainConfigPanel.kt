package com.aliothmoon.maameow.presentation.view.panel.depot

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.DepotMaintainPlan
import com.aliothmoon.maameow.data.model.DepotPlanOutcome
import com.aliothmoon.maameow.data.model.depotPlanOutcome
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.StageGroup
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.InlineActionRow
import com.aliothmoon.maameow.presentation.components.InlineConfirmPanel
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.view.panel.common.stageDisplayName
import com.aliothmoon.maameow.presentation.viewmodel.depotProgressText
import com.aliothmoon.maameow.theme.LocalReduceMotion
import com.aliothmoon.maameow.theme.MaaAnimatedVisibility
import com.aliothmoon.maameow.theme.MaaMotion
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 任务配置页里的库存保持面板：常规设置 / 高级设置两页。
 *
 * 两页的内容各自是独立组件（[DepotMaintainGeneralSection] / [DepotMaintainAdvancedSection]），
 * 库存数据页的计划面板也直接嵌同一份 —— 只有一个实现，两处的行为不会漂。
 */
@Composable
fun DepotMaintainConfigPanel(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    // 两页各自记滚动位置，切页来回不跳
    val generalScrollState = rememberScrollState()
    val advancedScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                R.string.common_tab_general,
                R.string.common_tab_advanced,
            ).forEachIndexed { page, labelRes ->
                val selected = pagerState.currentPage == page
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    // 用 selectable 而非 clickable，读屏才会播报选中态
                    modifier = Modifier.selectable(selected = selected, role = Role.Tab) {
                        coroutineScope.launch { pagerState.animateScrollToPage(page) }
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))

        HorizontalPager(
            pageSize = PageSize.Fill,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            // 页内容随 pager 一起被回收：切页回来时展开态、确认框自然是初始态，
            // 不必再单写一遍「切页收起」
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(if (page == 0) generalScrollState else advancedScrollState)
                    .padding(bottom = 8.dp),
            ) {
                when (page) {
                    0 -> DepotMaintainGeneralSection(
                        config = config,
                        onConfigChange = onConfigChange,
                    )

                    else -> DepotMaintainAdvancedSection(
                        config = config,
                        onConfigChange = onConfigChange,
                    )
                }
            }
        }
    }
}

/**
 * 常规设置：计划概览 + 增删/预设按钮 + 计划卡列表。
 *
 * 自带取数与局部展开态，调用方只要给配置和写回回调 —— 任务配置页的一页、
 * 库存数据页计划面板里的一段，用的都是这一份。
 */
@Composable
internal fun DepotMaintainGeneralSection(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
    modifier: Modifier = Modifier,
    depotRepository: DepotRepository = koinInject(),
    itemHelper: ItemHelper = koinInject(),
    activityManager: ActivityManager = koinInject(),
) {
    val snapshot by depotRepository.snapshot.collectAsStateWithLifecycle()
    // 物品下拉用「仓库识别认得出的那份」，名字映射用全表：老计划里可能存着已经不在
    // 列表里的 id，全表兜底才不会又显示成 ID
    val itemIndex by itemHelper.items.collectAsStateWithLifecycle()
    val activityStages by activityManager.activityStages.collectAsStateWithLifecycle()

    val stageGroups = rememberDepotStageGroups(activityManager)
    val stageCodes = remember(stageGroups) { stageGroups.allStageCodes() }
    val itemNameMap = remember(itemIndex) { itemIndex.mapValues { it.value.name } }
    val planOutcomes = remember(config.plans, snapshot, activityStages) {
        config.plans.map { plan ->
            depotPlanOutcome(plan, snapshot.items[plan.dropId] ?: 0) {
                activityManager.isStageOpen(it)
            }
        }
    }
    val itemIds = rememberDepotItemIds(itemHelper)

    // 展开态是纯 UI 局部状态，不持久化；删除时重映射下标，避免落到相邻计划。
    val expandedIndices = remember { mutableStateListOf<Int>() }
    var presetPanelExpanded by remember { mutableStateOf(false) }
    // 只在展开期间有效，应用或取消后清掉
    var selectedPreset by remember { mutableStateOf<DepotMaintainPreset?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GeneralTab(
            config = config,
            onConfigChange = onConfigChange,
            planOutcomes = planOutcomes,
            stageGroups = stageGroups,
            stageCodes = stageCodes,
            itemIds = itemIds,
            itemNameMap = itemNameMap,
            inventory = snapshot.items,
            inventorySynced = snapshot.syncTimeMillis != 0L,
            expandedIndices = expandedIndices,
            presetPanelExpanded = presetPanelExpanded,
            onPresetPanelExpandedChange = { presetPanelExpanded = it },
            selectedPreset = selectedPreset,
            onSelectedPresetChange = { selectedPreset = it },
            showClearConfirm = showClearConfirm,
            onShowClearConfirmChange = { showClearConfirm = it },
        )
    }
}

/** 高级设置：三组开关。任务配置页的一页、库存数据页计划面板里的一段共用这一份。 */
@Composable
internal fun DepotMaintainAdvancedSection(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvancedTab(config, onConfigChange)
    }
}

/**
 * 计划概览
 * 序号与计划卡、运行日志的编号同源（1 起、按原始位置），右侧给出本次会不会跑
 *
 * @param outcomes 与 [plans] 同序，由调用方算好，避免每次重组重跑判定
 */
@Composable
private fun PlanSummary(
    plans: List<DepotMaintainPlan>,
    outcomes: List<DepotPlanOutcome>,
    stageGroups: List<StageGroup>,
    itemNameMap: Map<String, String>,
    inventory: Map<String, Int>,
    inventorySynced: Boolean,
) {
    val notSelectedLabel = stringResource(R.string.panel_depot_not_selected)
    val runnableCount = outcomes.count { it == DepotPlanOutcome.Runnable }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.panel_depot_summary_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.panel_depot_summary_runnable,
                        runnableCount,
                        plans.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (runnableCount == 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
            )

            plans.forEachIndexed { index, plan ->
                val outcome = outcomes[index]
                val muted =
                    outcome != DepotPlanOutcome.Runnable && outcome != DepotPlanOutcome.Enough
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(
                        text = stageDisplayName(plan.stage, stageGroups) +
                                " · " + (itemNameMap[plan.dropId] ?: notSelectedLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    when (outcome) {
                        DepotPlanOutcome.NoItem -> SummaryTag(
                            text = stringResource(R.string.panel_depot_summary_no_item),
                            color = MaterialTheme.colorScheme.error,
                        )

                        DepotPlanOutcome.ZeroTarget -> SummaryTag(
                            text = stringResource(R.string.panel_depot_summary_zero_target),
                            color = MaterialTheme.colorScheme.error,
                        )

                        DepotPlanOutcome.StageRequired -> SummaryTag(
                            text = stringResource(R.string.panel_depot_summary_stage_required),
                            color = MaterialTheme.colorScheme.error,
                        )

                        DepotPlanOutcome.StageClosed -> SummaryTag(
                            text = stringResource(R.string.panel_depot_summary_stage_closed),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        DepotPlanOutcome.Enough -> SummaryTag(
                            text = stringResource(R.string.panel_depot_summary_enough),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        // 未识别过库存时用「--」表达「无数据」，而非误导性的 0
                        DepotPlanOutcome.Runnable -> SummaryTag(
                            text = depotProgressText(
                                current = inventory[plan.dropId] ?: 0,
                                target = plan.dropCount,
                                synced = inventorySynced,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTag(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
    )
}

/** 计划概览 + 增删按钮 + 计划卡列表；取数与展开态由 [DepotMaintainGeneralSection] 包住 */
@Composable
private fun ColumnScope.GeneralTab(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
    planOutcomes: List<DepotPlanOutcome>,
    stageGroups: List<StageGroup>,
    stageCodes: List<String>,
    itemIds: List<String>,
    itemNameMap: Map<String, String>,
    inventory: Map<String, Int>,
    inventorySynced: Boolean,
    expandedIndices: MutableList<Int>,
    presetPanelExpanded: Boolean,
    onPresetPanelExpandedChange: (Boolean) -> Unit,
    selectedPreset: DepotMaintainPreset?,
    onSelectedPresetChange: (DepotMaintainPreset?) -> Unit,
    showClearConfirm: Boolean,
    onShowClearConfirmChange: (Boolean) -> Unit,
) {
    if (config.plans.isNotEmpty()) {
        PlanSummary(
            plans = config.plans,
            outcomes = planOutcomes,
            stageGroups = stageGroups,
            itemNameMap = itemNameMap,
            inventory = inventory,
            inventorySynced = inventorySynced,
        )
    }
    // 三个即时操作横排；预设是展开器，独占一行贴着下面展开的面板
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                onConfigChange(config.copy(plans = config.plans + DepotMaintainPlan()))
            },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.panel_depot_add_plan))
        }
        OutlinedButton(
            onClick = { expandedIndices.clear() },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(
                Icons.Default.UnfoldLess,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.panel_depot_collapse_all))
        }
        OutlinedButton(
            onClick = {
                onShowClearConfirmChange(true)
                onPresetPanelExpandedChange(false)
            },
            modifier = Modifier.weight(1f),
            enabled = config.plans.isNotEmpty(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
        ) {
            Icon(
                Icons.Default.DeleteSweep,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.panel_depot_clear_plans))
        }
    }
    PresetPicker(
        expanded = presetPanelExpanded,
        onExpandedChange = {
            onPresetPanelExpandedChange(it)
            if (it) onShowClearConfirmChange(false)
        },
        selected = selectedPreset,
        onSelectedChange = onSelectedPresetChange,
        onApply = { preset ->
            onConfigChange(config.copy(plans = appendDepotMaintainPreset(config.plans, preset)))
        },
    )
    InlineConfirmPanel(
        visible = showClearConfirm && config.plans.isNotEmpty(),
        message = stringResource(
            R.string.panel_depot_clear_plans_confirm,
            config.plans.size,
        ),
        onConfirm = {
            expandedIndices.clear()
            onConfigChange(config.copy(plans = emptyList()))
            onShowClearConfirmChange(false)
        },
        onDismiss = { onShowClearConfirmChange(false) },
    )
    config.plans.forEachIndexed { index, plan ->
        PlanCard(
            plan = plan,
            expanded = index in expandedIndices,
            onToggleExpand = {
                if (index in expandedIndices) expandedIndices.remove(index)
                else expandedIndices.add(index)
            },
            customStageCode = config.customStageCode,
            showMedicine = config.useMedicine,
            showStone = config.useStone,
            stageGroups = stageGroups,
            stageCodes = stageCodes,
            itemIds = itemIds,
            itemNameMap = itemNameMap,
            onPlanChange = { updated ->
                onConfigChange(
                    config.copy(
                        plans = config.plans.toMutableList().also { it[index] = updated })
                )
            },
            onRemove = {
                // 删除 index 后：>index 的展开下标整体 -1，==index 移除
                val remapped = expandedIndices.mapNotNull { i ->
                    when {
                        i < index -> i
                        i == index -> null
                        else -> i - 1
                    }
                }.distinct()
                expandedIndices.clear()
                expandedIndices.addAll(remapped)
                onConfigChange(
                    config.copy(
                        plans = config.plans.toMutableList().also { it.removeAt(index) })
                )
            },
        )
    }
}

/** 预设选择器：点开后就地展开单选组，选中再确认才追加计划 */
@Composable
private fun ColumnScope.PresetPicker(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selected: DepotMaintainPreset?,
    onSelectedChange: (DepotMaintainPreset?) -> Unit,
    onApply: (DepotMaintainPreset) -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MaaMotion.spec(LocalReduceMotion.current, MaaMotion.Fast),
        label = "presetArrow",
    )

    OutlinedButton(
        onClick = {
            onExpandedChange(!expanded)
            if (expanded) onSelectedChange(null)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(stringResource(R.string.panel_depot_preset))
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .rotate(arrowRotation),
        )
    }
    MaaAnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                DepotMaintainPreset.entries.forEach { preset ->
                    val selected = selected == preset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                            ) { onSelectedChange(preset) }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(preset.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        // 提前告知会加几条，芯片预设一次进 8 条
                        Text(
                            text = stringResource(
                                R.string.panel_depot_preset_plan_count,
                                preset.plans.size,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                InlineActionRow(
                    confirmText = stringResource(R.string.panel_depot_preset_apply),
                    dismissText = stringResource(R.string.common_cancel),
                    confirmEnabled = selected != null,
                    onConfirm = {
                        selected?.let(onApply)
                        onExpandedChange(false)
                        onSelectedChange(null)
                    },
                    onDismiss = {
                        onExpandedChange(false)
                        onSelectedChange(null)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** 高级设置页：对齐上游 DepotMaintainTaskUserControl 的三个分组 */
@Composable
private fun ColumnScope.AdvancedTab(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
) {
    SectionHeader(stringResource(R.string.panel_depot_group_task_behavior))

    CheckBoxWithExpandableTip(
        checked = config.updateDepot,
        onCheckedChange = { onConfigChange(config.copy(updateDepot = it)) },
        label = stringResource(R.string.panel_depot_update_before_start),
        tipText = stringResource(R.string.panel_depot_update_before_start_tip),
    )

    CheckBoxWithLabel(
        checked = config.skipDuringActivity,
        onCheckedChange = { onConfigChange(config.copy(skipDuringActivity = it)) },
        label = stringResource(R.string.panel_depot_skip_during_activity),
    )

    CheckBoxWithExpandableTip(
        checked = config.onlyFirstInsufficientPlan,
        onCheckedChange = { onConfigChange(config.copy(onlyFirstInsufficientPlan = it)) },
        label = stringResource(R.string.panel_depot_only_first_insufficient_plan),
        tipText = stringResource(R.string.panel_depot_only_first_insufficient_plan_tip),
    )

    CheckBoxWithLabel(
        checked = config.skipDuringResourceCollection,
        onCheckedChange = { onConfigChange(config.copy(skipDuringResourceCollection = it)) },
        label = stringResource(R.string.panel_depot_skip_during_resource),
    )

    SectionHeader(
        title = stringResource(R.string.panel_depot_group_stage_battle),
        modifier = Modifier.padding(top = 4.dp),
    )

    CheckBoxWithExpandableTip(
        checked = config.customStageCode,
        onCheckedChange = { onConfigChange(config.copy(customStageCode = it)) },
        label = stringResource(R.string.panel_fight_custom_stage_code),
        tipText = stringResource(R.string.panel_fight_custom_stage_code_tip),
    )

    CheckBoxWithExpandableTip(
        checked = config.useAutoSeries,
        onCheckedChange = { onConfigChange(config.copy(useAutoSeries = it)) },
        label = stringResource(R.string.panel_depot_use_auto_series),
        tipText = stringResource(R.string.panel_depot_use_auto_series_tip),
    )

    SectionHeader(
        title = stringResource(R.string.panel_depot_group_sanity),
        modifier = Modifier.padding(top = 4.dp),
    )

    CheckBoxWithLabel(
        checked = config.useMedicine,
        onCheckedChange = { onConfigChange(config.copy(useMedicine = it)) },
        label = stringResource(R.string.panel_depot_enable_use_medicine),
    )

    CheckBoxWithLabel(
        checked = config.useStone,
        onCheckedChange = { onConfigChange(config.copy(useStone = it)) },
        label = stringResource(R.string.panel_depot_enable_use_stone),
    )

    CheckBoxWithExpandableTip(
        checked = config.useExpiringMedicine,
        onCheckedChange = { onConfigChange(config.copy(useExpiringMedicine = it)) },
        label = stringResource(R.string.panel_depot_use_expiring_medicine),
        tipText = stringResource(R.string.panel_depot_use_expiring_medicine_tip),
    )
}

@Composable
private fun PlanCard(
    plan: DepotMaintainPlan,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    customStageCode: Boolean,
    showMedicine: Boolean,
    showStone: Boolean,
    stageGroups: List<StageGroup>,
    stageCodes: List<String>,
    itemIds: List<String>,
    itemNameMap: Map<String, String>,
    onPlanChange: (DepotMaintainPlan) -> Unit,
    onRemove: () -> Unit,
) {
    val notSelectedLabel = stringResource(R.string.panel_depot_not_selected)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stageDisplayName(
                        plan.stage,
                        stageGroups,
                    ) + " · ${itemNameMap[plan.dropId] ?: notSelectedLabel}" + " x${plan.dropCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            MaaAnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 字段本体与库存数据页共用一份，见 DepotPlanFields
                    DepotPlanFields(
                        plan = plan,
                        onPlanChange = onPlanChange,
                        stageGroups = stageGroups,
                        stageCodes = stageCodes,
                        customStageCode = customStageCode,
                        onCustomStageSelected = {},
                        showMedicine = showMedicine,
                        showStone = showStone,
                        itemIds = itemIds,
                        itemNameMap = itemNameMap,
                    )

                    HorizontalDivider()

                    // 移动端无 hover，删除按钮在展开态底部常驻（上游为 hover 显示）
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.panel_depot_remove_plan))
                    }
                }
            }
        }
    }
}
