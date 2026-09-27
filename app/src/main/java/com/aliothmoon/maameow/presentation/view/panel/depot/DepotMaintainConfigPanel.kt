package com.aliothmoon.maameow.presentation.view.panel.depot

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import com.aliothmoon.maameow.presentation.components.CheckBoxWithExpandableTip
import com.aliothmoon.maameow.presentation.components.CheckBoxWithLabel
import com.aliothmoon.maameow.presentation.components.InlineActionRow
import com.aliothmoon.maameow.presentation.components.InlineConfirmPanel
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.theme.LocalReduceMotion
import com.aliothmoon.maameow.theme.MaaAnimatedVisibility
import com.aliothmoon.maameow.theme.MaaDesignTokens
import com.aliothmoon.maameow.theme.MaaMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 任务配置页里的库存保持面板。
 *
 * 只有常规设置这一页：原来跟它并排的「高级设置」页已经收进计划详情面板 —— 点任意一格计划，
 * 面板里保存按钮下面就是同一份 [DepotMaintainAdvancedSection]。同一个设置不再摆两个入口，
 * 少一页也就少一次「到底该在哪页改」的判断。
 *
 * 取数、预设展开态、计划详情面板都在这儿。
 *
 * @param nodeEnabled 所属节点是否启用，计划详情面板里据此提示「保存了也不会跑」
 */
@Composable
fun DepotMaintainConfigPanel(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
    nodeEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    depotRepository: DepotRepository = koinInject(),
    itemHelper: ItemHelper = koinInject(),
    activityManager: ActivityManager = koinInject(),
    iconLoader: ItemIconLoader = koinInject(),
) {
    val snapshot by depotRepository.snapshot.collectAsStateWithLifecycle()
    // 物品下拉用「仓库识别认得出的那份」，名字映射用全表：老计划里可能存着已经不在
    // 列表里的 id，全表兜底才不会又显示成 ID
    val itemIndex by itemHelper.items.collectAsStateWithLifecycle()
    val activityStages by activityManager.activityStages.collectAsStateWithLifecycle()

    val itemNameMap = remember(itemIndex) { itemIndex.mapValues { it.value.name } }
    val planOutcomes = remember(config.plans, snapshot, activityStages) {
        config.plans.map { plan ->
            depotPlanOutcome(plan, snapshot.items[plan.dropId] ?: 0) {
                activityManager.isStageOpen(it)
            }
        }
    }
    val itemIds = rememberDepotItemIds(itemHelper)

    var presetPanelExpanded by remember { mutableStateOf(false) }
    // 只在展开期间有效，应用或取消后清掉
    var selectedPreset by remember { mutableStateOf<DepotMaintainPreset?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    // 点开的那一格；planIndex 为空 = 还没有计划，面板里是新建
    var sheetTarget by remember { mutableStateOf<DepotPlanSheetTarget?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GeneralTab(
            config = config,
            onConfigChange = onConfigChange,
            planOutcomes = planOutcomes,
            itemIds = itemIds,
            itemNameMap = itemNameMap,
            inventory = snapshot.items,
            inventorySynced = snapshot.syncTimeMillis != 0L,
            presetPanelExpanded = presetPanelExpanded,
            onPresetPanelExpandedChange = { presetPanelExpanded = it },
            selectedPreset = selectedPreset,
            onSelectedPresetChange = { selectedPreset = it },
            showClearConfirm = showClearConfirm,
            onShowClearConfirmChange = { showClearConfirm = it },
            iconLoader = iconLoader,
            onOpenPlan = { sheetTarget = it },
        )
    }

    // 详情面板挂在滚动容器外面：它自己就是一层模态，跟着页面滚没有意义
    sheetTarget?.let { target ->
        val index = target.planIndex
        DepotPlanEditorSheet(
            itemId = target.itemId,
            itemName = target.itemName,
            count = snapshot.items[target.itemId] ?: 0,
            existing = index?.let { config.plans.getOrNull(it) },
            existingOutcome = index?.let { planOutcomes.getOrNull(it) },
            context = DepotPlanContext(nodeEnabled = nodeEnabled),
            config = config,
            synced = snapshot.syncTimeMillis != 0L,
            onDismiss = { sheetTarget = null },
            onSave = { plan ->
                val plans = config.plans.toMutableList()
                if (index != null && index in plans.indices) plans[index] = plan else plans.add(plan)
                onConfigChange(config.copy(plans = plans))
            },
            onRemove = {
                val plans = config.plans.toMutableList()
                if (index != null && index in plans.indices) plans.removeAt(index)
                onConfigChange(config.copy(plans = plans))
            },
            onConfigChange = onConfigChange,
        )
    }
}

/**
 * 高级设置：三组开关。
 *
 * 计划详情面板里的一段 —— 面板点格子进的就是那一个 [DepotPlanEditorSheet]，
 * 所以这份设置也只有一处实现。
 */
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
 * 计划概览：一条计划一行，拖右侧手柄换位、向左划开点删除，排出来的顺序就是执行顺序。
 *
 * 行的样子、进度口径、去向文案都只有一份实现。这里天然只有一个库存保持节点，
 * 不用分段；拖完直接写回本节点的 [DepotMaintainConfig.plans]。
 *
 * 序号与计划卡、运行日志的编号同源（1 起、按原始位置）。
 *
 * 删除是「划开 → 点红块 → 两秒可撤回」：划开本身就已经是一次确认（还带一次触感），
 * 再弹一句「确定吗」比划掉本身还碍事；撤销窗口里那条计划原样躺着，
 * 点一下 [PlanRemovalUndoBar] 就回原位。
 *
 * @param outcomes 与 [plans] 同序且等长，由调用方算好，避免每次重组重跑判定
 * @param onMove 松手后回调：第 [from] 条挪到了第 [to] 位
 * @param onDelete 划开并点了删除：删掉第 [index] 条
 * @param onRestore 撤销：把 [plan] 放回第 [index] 位
 */
@Composable
private fun PlanOverview(
    plans: List<DepotMaintainPlan>,
    outcomes: List<DepotPlanOutcome>,
    itemNameMap: Map<String, String>,
    inventory: Map<String, Int>,
    inventorySynced: Boolean,
    iconLoader: ItemIconLoader,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: (Int) -> Unit,
    onRestore: (index: Int, plan: DepotMaintainPlan) -> Unit,
) {
    val notSelectedLabel = stringResource(R.string.panel_depot_not_selected)
    val entries = remember(plans, outcomes, inventory, itemNameMap, notSelectedLabel) {
        depotPlanOrderEntries(
            plans = plans,
            outcomes = outcomes,
            inventory = inventory,
            itemNameMap = itemNameMap,
            notSelectedLabel = notSelectedLabel,
        )
    }
    val runnableCount = outcomes.count { it == DepotPlanOutcome.Runnable }

    // 划掉的那一条。撤销之后不清空：收起动画还在播的时候文案不该先空掉，
    // 而它只在撤销窗口里被读，留着不会有别的副作用
    var pendingRemoval by remember { mutableStateOf<DepotPlanRemoval?>(null) }
    var undoVisible by remember { mutableStateOf(false) }
    var undoJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val removePlan: (Int) -> Unit = { index ->
        plans.getOrNull(index)?.let { plan ->
            pendingRemoval = DepotPlanRemoval(
                index = index,
                plan = plan,
                name = itemNameMap[plan.dropId] ?: notSelectedLabel,
            )
            undoVisible = true
            // 又划掉一条就重新计时：上一个计时器到点也不该把新的这条收掉
            undoJob?.cancel()
            undoJob = scope.launch {
                delay(DepotPlanUndoMillis)
                undoVisible = false
            }
            onDelete(index)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // 列表空的时候不画汇总：0/0 那一行说明不了任何事
        if (plans.isNotEmpty()) {
            // 汇总压成一行小字：逐条的去向下面每一行自己会讲，这里只回答「这一趟会不会白跑」
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
            Text(
                text = stringResource(R.string.depot_inventory_order_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PlanRemovalUndoBar(
            name = pendingRemoval?.name,
            visible = undoVisible,
            onUndo = {
                undoVisible = false
                undoJob?.cancel()
                pendingRemoval?.let { onRestore(it.index, it.plan) }
            },
        )
        DepotPlanOrderColumn(
            entries = entries,
            synced = inventorySynced,
            iconLoader = iconLoader,
            onMove = onMove,
            onDelete = removePlan,
        )
    }
}

/** 刚划掉、还能撤回的那一条：计划本体 + 原来的位置 + 列表里显示的名字 */
private data class DepotPlanRemoval(
    val index: Int,
    val plan: DepotMaintainPlan,
    val name: String,
)

/** 划掉之后「撤销」亮多久 */
private const val DepotPlanUndoMillis = 2_000L

/**
 * 划掉一条计划之后浮出来的撤销条：左边说划掉了谁，右边一个「撤销」。
 *
 * 就摆在列表上方 —— 划的是列表里的行，提示也在同一片区域里，不用去屏幕角落找。
 * 到点自己收起来（[DepotPlanUndoMillis]），所以不占地方、也不需要「知道了」。
 *
 * @param name 被划掉的那条计划叫什么；null = 还没划过，收起状态
 */
@Composable
private fun PlanRemovalUndoBar(
    name: String?,
    visible: Boolean,
    onUndo: () -> Unit,
) {
    // 一直留在组合里（收起时内容不组合，见 MaaAnimatedVisibility）：第一次划掉时
    // visible 是由 false 翻上来的，展开动效才播得出来；要是这一刻才第一次组合，
    // AnimatedVisibility 的初值就是「已经可见」，整条会直接蹦出来
    MaaAnimatedVisibility(visible = visible) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = MaaDesignTokens.Spacing.md, end = MaaDesignTokens.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.panel_depot_plan_removed, name.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onUndo) {
                    Text(stringResource(R.string.panel_depot_undo_remove))
                }
            }
        }
    }
}

/** 计划概览 + 清空/预设 + 计划格子；取数与计划详情面板由 [DepotMaintainConfigPanel] 包住 */
@Composable
private fun ColumnScope.GeneralTab(
    config: DepotMaintainConfig,
    onConfigChange: (DepotMaintainConfig) -> Unit,
    planOutcomes: List<DepotPlanOutcome>,
    itemIds: List<String>,
    itemNameMap: Map<String, String>,
    inventory: Map<String, Int>,
    inventorySynced: Boolean,
    presetPanelExpanded: Boolean,
    onPresetPanelExpandedChange: (Boolean) -> Unit,
    selectedPreset: DepotMaintainPreset?,
    onSelectedPresetChange: (DepotMaintainPreset?) -> Unit,
    showClearConfirm: Boolean,
    onShowClearConfirmChange: (Boolean) -> Unit,
    iconLoader: ItemIconLoader,
    onOpenPlan: (DepotPlanSheetTarget) -> Unit,
) {
    // 列表空的时候也照样组合：撤销条得在「最后一条被划掉」时还活着，
    // 不然划掉最后一条就没有回头路了
    PlanOverview(
        plans = config.plans,
        outcomes = planOutcomes,
        itemNameMap = itemNameMap,
        inventory = inventory,
        inventorySynced = inventorySynced,
        iconLoader = iconLoader,
        onMove = { from, to ->
            // 拖动改的就是执行顺序：执行侧按节点内 plans 的下标依次跑
            onConfigChange(
                config.copy(
                    plans = config.plans.toMutableList().also { it.add(to, it.removeAt(from)) }
                )
            )
        },
        onDelete = { index ->
            val plans = config.plans.toMutableList()
            if (index in plans.indices) plans.removeAt(index)
            onConfigChange(config.copy(plans = plans))
        },
        onRestore = { index, plan ->
            val plans = config.plans.toMutableList()
            // 撤销窗口里列表还可能被拖动过，原位放不下就贴到末尾
            plans.add(index.coerceIn(0, plans.size), plan)
            onConfigChange(config.copy(plans = plans))
        },
    )
    // 加计划不再有按钮：点下面「暂无计划」里的图标，进的就是同一个详情面板
    OutlinedButton(
        onClick = {
            onShowClearConfirmChange(true)
            onPresetPanelExpandedChange(false)
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = config.plans.isNotEmpty(),
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
            onConfigChange(config.copy(plans = emptyList()))
            onShowClearConfirmChange(false)
        },
        onDismiss = { onShowClearConfirmChange(false) },
    )
    MaintainPlanGrid(
        plans = config.plans,
        itemIds = itemIds,
        itemNameMap = itemNameMap,
        inventory = inventory,
        synced = inventorySynced,
        iconLoader = iconLoader,
        onOpenPlan = onOpenPlan,
    )
}

/** 要点开哪一条计划的详情设置；[planIndex] 为空 = 这一格还没有计划，点开是新建 */
private data class DepotPlanSheetTarget(
    val itemId: String,
    val itemName: String,
    val planIndex: Int?,
)

/**
 * 预设下面那两组格子：已经有计划的在上，还没有计划的在下。
 *
 * 上面那组一条计划一格，顺序就是计划概览、刷取顺序的顺序；下面那组是物品表里其余的全部材料，
 * 默认收起（上百个材料不该一进来就铺满一屏），点标题行展开。「添加」按钮去掉之后，
 * 加一条计划就是点下面那组里的图标 —— 进的都是同一个详情面板（见 [DepotPlanEditorSheet]）。
 */
@Composable
private fun MaintainPlanGrid(
    plans: List<DepotMaintainPlan>,
    itemIds: List<String>,
    itemNameMap: Map<String, String>,
    inventory: Map<String, Int>,
    synced: Boolean,
    iconLoader: ItemIconLoader,
    onOpenPlan: (DepotPlanSheetTarget) -> Unit,
) {
    val notSelectedLabel = stringResource(R.string.panel_depot_not_selected)
    val groups = remember(plans, itemIds, itemNameMap, inventory, notSelectedLabel) {
        depotMaintainItemCells(
            plans = plans,
            itemIds = itemIds,
            itemNameMap = itemNameMap,
            inventory = inventory,
            notSelectedLabel = notSelectedLabel,
        )
    }
    // 收起态是纯 UI 局部状态：切页回来时页内容会被回收，正好回到默认的收起
    var unplannedCollapsed by rememberSaveable { mutableStateOf(true) }
    val sectionColor = MaterialTheme.colorScheme.onSurfaceVariant
    val openCell: (DepotItemCellUi) -> Unit = { cell ->
        onOpenPlan(
            DepotPlanSheetTarget(
                itemId = cell.itemId,
                itemName = cell.name,
                planIndex = cell.planIndex,
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs)) {
        if (groups.planned.isNotEmpty()) {
            DepotSectionBreak(
                textRes = R.string.panel_depot_section_planned,
                count = groups.planned.size,
                color = sectionColor,
            )
            DepotItemFlow(
                cells = groups.planned,
                synced = synced,
                iconLoader = iconLoader,
                onClick = openCell,
            )
        }
        DepotSectionBreak(
            textRes = R.string.panel_depot_section_unplanned,
            count = groups.unplanned.size,
            color = sectionColor,
            collapsed = unplannedCollapsed,
            onToggle = { unplannedCollapsed = !unplannedCollapsed },
        )
        // 收起时整片不组合：上百个图标没必要在看不见的时候也去解码
        MaaAnimatedVisibility(visible = !unplannedCollapsed) {
            DepotItemFlow(
                cells = groups.unplanned,
                synced = synced,
                iconLoader = iconLoader,
                onClick = openCell,
            )
        }
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
