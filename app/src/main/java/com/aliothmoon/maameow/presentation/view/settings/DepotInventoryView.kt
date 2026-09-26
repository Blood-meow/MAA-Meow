package com.aliothmoon.maameow.presentation.view.settings

import android.text.InputType
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.DepotMaintainPlan
import com.aliothmoon.maameow.data.model.DepotPlanOutcome
import com.aliothmoon.maameow.data.model.LogColorRole
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportLabels
import com.aliothmoon.maameow.data.repository.OperBoxSnapshot
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import com.aliothmoon.maameow.data.resource.StageAliasMapper
import com.aliothmoon.maameow.domain.enums.UiUsageConstants
import com.aliothmoon.maameow.domain.service.ToolboxExportFileType
import com.aliothmoon.maameow.presentation.components.ITextFieldWithFocus
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.view.panel.OperatorRow
import com.aliothmoon.maameow.presentation.view.panel.ToolboxFileExporter
import com.aliothmoon.maameow.presentation.view.panel.common.GroupedStageButtonGroup
import com.aliothmoon.maameow.presentation.view.panel.rememberOperBoxExportLabels
import com.aliothmoon.maameow.presentation.view.panel.rememberSafToolboxFileExporter
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryCellUi
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryViewModel
import com.aliothmoon.maameow.presentation.viewmodel.DepotMaintainPlanUi
import com.aliothmoon.maameow.presentation.viewmodel.DepotPlanContext
import com.aliothmoon.maameow.presentation.viewmodel.DepotProfileRow
import com.aliothmoon.maameow.theme.MaaDesignTokens
import com.aliothmoon.maameow.theme.MaaMotion
import com.aliothmoon.maameow.theme.OpaqueTheme
import com.aliothmoon.maameow.theme.themedColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** 目标库存上限，对齐 WPF NumericUpDown 的 Maximum（与任务配置页同一个数） */
private const val MAX_TARGET_INVENTORY = 1145141919

/**
 * 库存数据：二级配置档列表 → 三级配置档详情。
 *
 * 仓库/干员快照按配置档分片，一档一份；三级页把「库存保持」计划并进库存格子，
 * 物品的当前库存就是计划进度，点格子直接改目标库存。
 */
@Composable
fun DepotInventoryView(
    navController: NavController,
    viewModel: DepotInventoryViewModel = koinViewModel(),
) {
    val rows by viewModel.profileRows.collectAsStateWithLifecycle()
    val selectedProfileId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val exporter = rememberSafToolboxFileExporter()
    val exportLabels = rememberOperBoxExportLabels()
    // 导出协程挂在页面上而不是面板上：渲染要几秒，面板一收就取消会把导出吞掉
    val exportScope = rememberCoroutineScope()
    var exportProfileId by remember { mutableStateOf<String?>(null) }
    var pendingClearProfileId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedProfileId.isNotEmpty()) { viewModel.clearSelection() }

    AnimatedContent(
        targetState = selectedProfileId,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState.isNotEmpty()
            val spec = tween<IntOffset>(220, easing = MaaMotion.Emphasized)
            val enter = slideInHorizontally(animationSpec = spec) { if (forward) it / 5 else -it / 5 } +
                fadeIn(tween(200))
            val exit = slideOutHorizontally(animationSpec = spec) { if (forward) -it / 5 else it / 5 } +
                fadeOut(tween(160))
            enter togetherWith exit
        },
        label = "depot-inventory-navigation",
    ) { profileId ->
        if (profileId.isEmpty()) {
            DepotProfileListView(
                rows = rows,
                onBack = { navController.navigateUp() },
                onOpen = viewModel::selectProfile,
                onExport = { exportProfileId = it },
                onClear = { pendingClearProfileId = it },
            )
        } else {
            DepotProfileDetailView(
                row = rows.firstOrNull { it.id == profileId },
                profileName = rows.firstOrNull { it.id == profileId }?.name ?: profileId,
                onBack = { viewModel.clearSelection() },
                onExport = { exportProfileId = profileId },
                onClear = { pendingClearProfileId = profileId },
                viewModel = viewModel,
            )
        }
    }

    exportProfileId?.let { id ->
        // 玻璃背景下面板会透出底下的内容，弹层统一换回不透明配色
        OpaqueTheme {
            DepotInventoryExportBottomSheet(
                profileId = id,
                titleLabel = rows.firstOrNull { it.id == id }?.name ?: id,
                exportScope = exportScope,
                onDismiss = { exportProfileId = null },
                viewModel = viewModel,
                exporter = exporter,
                exportLabels = exportLabels,
            )
        }
    }

    pendingClearProfileId?.let { id ->
        val name = rows.firstOrNull { it.id == id }?.name ?: id
        // 与其它确认框一致：玻璃配色下弹窗底色是半透明的，会透出底下的库存网格
        OpaqueTheme {
            AlertDialog(
                onDismissRequest = { pendingClearProfileId = null },
                title = { Text(stringResource(R.string.depot_inventory_delete_title)) },
                text = { Text(stringResource(R.string.depot_inventory_delete_message, name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearProfile(id)
                            pendingClearProfileId = null
                        },
                    ) {
                        Text(
                            stringResource(R.string.depot_inventory_delete_confirm),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingClearProfileId = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        }
    }
}

// ============================== 二级：配置档列表 ==============================

@Composable
private fun DepotProfileListView(
    rows: List<DepotProfileRow>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onExport: (String) -> Unit,
    onClear: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.depot_inventory_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.depot_inventory_empty_profiles),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = MaaDesignTokens.Spacing.listHorizontal,
                        vertical = MaaDesignTokens.Spacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm),
                ) {
                    items(rows, key = { it.id }) { row ->
                        SwipeRevealProfileCard(
                            row = row,
                            onClick = { onOpen(row.id) },
                            onExport = { onExport(row.id) },
                            onClear = { onClear(row.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 左滑露出「导出 / 清空」的配置档卡片。
 *
 * 操作钮只铺在露出的那条窄带里，卡片用 surfaceVariant 也不会透出整块按钮。
 */
@Composable
private fun SwipeRevealProfileCard(
    row: DepotProfileRow,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 144.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    // 拖动期间用 snap 直接跟手，松手才切成 tween 收敛：
    // 不在每个触摸事件里起一个协程去 snapTo 一个 Animatable
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val offsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = if (dragging) snap() else tween(180),
        label = "swipe-reveal-offset",
    )
    // 用卡片实测高度，保证按钮与卡片完全等高
    var cardHeightPx by remember { mutableIntStateOf(0) }
    val revealPx = (-offsetX).coerceIn(0f, actionWidthPx)
    val revealDp = with(density) { revealPx.toDp() }
    val cardHeightDp = with(density) { cardHeightPx.toDp() }
    val revealed = revealPx > 8f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaaDesignTokens.CornerRadius.card)),
    ) {
        if (cardHeightPx > 0 && revealPx > 0.5f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(cardHeightDp)
                    .width(revealDp)
                    .clip(RectangleShape),
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(actionWidth),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = revealed, onClick = onExport),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.depot_inventory_export),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.error)
                            .clickable(enabled = revealed, onClick = onClear),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.depot_inventory_delete),
                            tint = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { cardHeightPx = it.height }
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(actionWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            dragOffsetX = if (dragOffsetX < -actionWidthPx / 2f) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        },
                        onDragCancel = { dragging = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX = (dragOffsetX + dragAmount)
                                .coerceIn(-actionWidthPx, 0f)
                        },
                    )
                }
                .clickable {
                    if (dragOffsetX < -8f) {
                        dragging = false
                        dragOffsetX = 0f
                    } else {
                        onClick()
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(MaaDesignTokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm),
                ) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.isActive) {
                        Text(
                            text = stringResource(R.string.depot_inventory_profile_active),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = row.maintainSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.hasSynced && row.unmetCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (row.syncTimeMillis > 0L) {
                    Text(
                        text = stringResource(
                            R.string.depot_inventory_sync_time,
                            DateFormat.getDateTimeInstance()
                                .format(Date(row.syncTimeMillis)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 卡片上的保持进度文案：没识别过 / 没配计划 / 全部已够 / 还差几项。
 *
 * 没有库存快照时先说「没数据」——那种情况下所有计划都会算成未集齐，
 * 直接报「N 项未集齐」会让人以为真缺这么多。
 */
@Composable
private fun DepotProfileRow.maintainSummary(): String = when {
    !hasSynced -> stringResource(R.string.depot_inventory_empty_profile)
    planCount == 0 -> stringResource(R.string.depot_inventory_maintain_none)
    unmetCount == 0 -> stringResource(R.string.depot_inventory_maintain_all_done)
    else -> stringResource(R.string.depot_inventory_maintain_unmet_count, unmetCount)
}

// ============================== 三级：配置档详情 ==============================

@Composable
private fun DepotProfileDetailView(
    row: DepotProfileRow?,
    profileName: String,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    viewModel: DepotInventoryViewModel,
) {
    val cells by viewModel.cells.collectAsStateWithLifecycle()
    val operBox by viewModel.operBoxSnapshot.collectAsStateWithLifecycle()
    val planContext by viewModel.planContext.collectAsStateWithLifecycle()
    var planSheetItemId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.depot_inventory_detail_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.depot_inventory_export),
                        )
                    }
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.depot_inventory_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaaDesignTokens.Spacing.md,
                        vertical = MaaDesignTokens.Spacing.xs,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
            ) {
                Text(
                    text = profileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row?.let {
                    if (it.planCount > 0 || !it.hasSynced) {
                        Text(
                            text = it.maintainSummary(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.hasSynced && it.unmetCount > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (it.syncTimeMillis > 0L) {
                        Text(
                            text = stringResource(
                                R.string.depot_inventory_sync_time,
                                DateFormat.getDateTimeInstance()
                                    .format(Date(it.syncTimeMillis)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            InventoryDetailBody(
                cells = cells,
                operBox = operBox,
                onCellClick = { planSheetItemId = it.id },
            )
        }
    }

    planSheetItemId?.let { itemId ->
        cells.firstOrNull { it.id == itemId }?.let { cell ->
            DepotMaintainPlanSheet(
                cell = cell,
                context = cell.plan?.let { DepotPlanContext(nodeEnabled = it.nodeEnabled) }
                    ?: planContext,
                onDismiss = { planSheetItemId = null },
                // 用点开时那一格上的计划当写回落点，不回查派生流
                onSave = { plan -> viewModel.savePlan(cell.plan, plan) },
                onRemove = { viewModel.removePlan(cell.plan) },
            )
        }
    }
}

/** Tab 行与 pager 必须上下排布：同处一个 Box 时 pager 会盖住 Tab，导致点不动。 */
@Composable
private fun InventoryDetailBody(
    cells: List<DepotInventoryCellUi>,
    operBox: OperBoxSnapshot,
    onCellClick: (DepotInventoryCellUi) -> Unit,
    iconLoader: ItemIconLoader = koinInject(),
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    // 放在 pager 外面：pager 会把滑走的页面回收掉，放里面一滑回来选择就没了
    var operBoxTab by remember { mutableIntStateOf(0) }
    val pageLabels = listOf(
        stringResource(R.string.depot_inventory_tab_items),
        stringResource(R.string.depot_inventory_tab_operators),
    )
    Column(modifier = Modifier.fillMaxSize()) {
        DetailPageTabs(
            labels = pageLabels,
            selectedIndex = pagerState.currentPage,
            onSelected = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = true,
        ) { page ->
            when (page) {
                0 -> DepotItemsPage(
                    cells = cells,
                    iconLoader = iconLoader,
                    onCellClick = onCellClick,
                )

                else -> OperBoxPage(
                    snapshot = operBox,
                    selectedTab = operBoxTab,
                    onTabChange = { operBoxTab = it },
                )
            }
        }
    }
}

@Composable
private fun DetailPageTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaaDesignTokens.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xxl),
    ) {
        labels.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedIndex == index) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelected(index) }
                    .padding(vertical = MaaDesignTokens.Spacing.sm),
            )
        }
    }
}

/**
 * 库存格子网格。
 *
 * 未集齐的物品单独成组，用一条分隔行跟前面「已集齐 / 不需要集齐」的格子隔开，
 * 免得缺货的东西埋在几十个已够的物品里找不到。
 */
@Composable
private fun DepotItemsPage(
    cells: List<DepotInventoryCellUi>,
    iconLoader: ItemIconLoader,
    onCellClick: (DepotInventoryCellUi) -> Unit,
) {
    if (cells.isEmpty()) {
        DetailEmptyText(R.string.depot_inventory_empty_items)
        return
    }
    // 一次遍历分组，且只在 cells 真变了才重算（否则每次重组都白跑两遍 filter）
    val (settled, unmet) = remember(cells) { cells.partition { !it.unmet } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 92.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaaDesignTokens.Spacing.md),
        contentPadding = PaddingValues(top = 6.dp, bottom = MaaDesignTokens.Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(settled, key = { "settled-${it.id}" }) { cell ->
            InventoryItemCell(cell = cell, iconLoader = iconLoader, onClick = onCellClick)
        }
        if (unmet.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "unmet-break") {
                UnmetSectionBreak(count = unmet.size)
            }
            items(unmet, key = { "unmet-${it.id}" }) { cell ->
                InventoryItemCell(cell = cell, iconLoader = iconLoader, onClick = onCellClick)
            }
        }
    }
}

/** 「未集齐」分隔行：一条横线加组标题，把网格断成两段。 */
@Composable
private fun UnmetSectionBreak(count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaaDesignTokens.Spacing.sm),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        Text(
            text = stringResource(R.string.depot_inventory_section_unmet, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(
                top = MaaDesignTokens.Spacing.xs,
                bottom = 2.dp,
            ),
        )
    }
}

/**
 * 一格库存：图标下面是「目标/当前」，未集齐标红、已集齐标绿。
 *
 * 库存和库存保持是同一件事，所以不分成两个区块：没配计划的物品显示纯数量 xN，
 * 配了计划的直接显示目标与当前，红色就代表还差。
 */
@Composable
private fun InventoryItemCell(
    cell: DepotInventoryCellUi,
    iconLoader: ItemIconLoader,
    onClick: (DepotInventoryCellUi) -> Unit,
) {
    val plan = cell.plan
    val unmet = cell.unmet
    val accent = when {
        plan == null -> MaterialTheme.colorScheme.onSurfaceVariant
        unmet -> MaterialTheme.colorScheme.error
        else -> LogColorRole.SUCCESS.themedColor()
    }
    Surface(
        shape = RoundedCornerShape(MaaDesignTokens.CornerRadius.inner),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = plan?.let { BorderStroke(1.dp, accent.copy(alpha = 0.5f)) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(cell) },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ItemIcon(itemId = cell.id, contentDescription = cell.name, size = 44.dp, loader = iconLoader)
            Text(
                text = cell.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (plan == null) {
                Text(
                    text = "${cell.count}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                // 格子只有 ~80dp 宽，目标能到 10 位；不省略号的话会被从中间硬裁
                Text(
                    text = "${plan.target}/${cell.count}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (unmet) {
                    Text(
                        text = stringResource(R.string.depot_inventory_maintain_need, plan.need),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemIcon(
    itemId: String,
    contentDescription: String?,
    size: Dp,
    loader: ItemIconLoader,
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, itemId) {
        value = loader.load(itemId)
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = Modifier
                .height(size)
                .width(size),
        )
    } else {
        Spacer(Modifier.size(size))
    }
}

@Composable
private fun OperBoxPage(
    snapshot: OperBoxSnapshot,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
) {
    if (!snapshot.hasSynced) {
        DetailEmptyText(R.string.depot_inventory_empty_operators)
        return
    }
    val operators = if (selectedTab == 0) snapshot.owned else snapshot.notOwned
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaaDesignTokens.Spacing.md),
        contentPadding = PaddingValues(top = 6.dp, bottom = MaaDesignTokens.Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.lg),
            ) {
                val labels = listOf(
                    stringResource(R.string.panel_operbox_tab_owned, snapshot.owned.size),
                    stringResource(R.string.panel_operbox_tab_not_owned, snapshot.notOwned.size),
                )
                labels.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedTab == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selectedTab == index) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onTabChange(index) }
                            .padding(vertical = MaaDesignTokens.Spacing.xs),
                    )
                }
            }
        }
        items(operators, key = { it.id }) { oper -> OperatorRow(oper) }
    }
}

@Composable
private fun DetailEmptyText(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaaDesignTokens.Spacing.xxl),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

// ============================== 库存保持设置面板 ==============================

/**
 * 点库存格子后弹出的库存保持设置。
 *
 * 物品已经由点击定死，这里只改「保到多少、去哪刷」；
 * 保存时写回该配置档的库存保持节点，没有节点就新建一个。
 * 保存/移除都先把面板滑下去再落库，动画与展开时对称。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepotMaintainPlanSheet(
    cell: DepotInventoryCellUi,
    context: DepotPlanContext,
    onDismiss: () -> Unit,
    onSave: (DepotMaintainPlan) -> Unit,
    onRemove: () -> Unit,
    itemHelper: ItemHelper = koinInject(),
    activityManager: ActivityManager = koinInject(),
    iconLoader: ItemIconLoader = koinInject(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dropItems by itemHelper.dropItems.collectAsStateWithLifecycle()
    val activityStages by activityManager.activityStages.collectAsStateWithLifecycle()

    // 排除「当期剿灭」与「当前/上次」：库存保持按材料刷，这两类算不出缺口
    val stageGroups = remember(activityStages) {
        activityManager.getMergedStageGroups()
            .map { group ->
                group.copy(stages = group.stages.filterNot {
                    it.code == "Annihilation" || it.code.isEmpty()
                })
            }
            .filter { it.stages.isNotEmpty() }
    }
    val stageCodes = remember(stageGroups) {
        stageGroups.flatMap { group -> group.stages.map { it.code } }
    }
    val itemIds = remember(dropItems) {
        if (dropItems.isNotEmpty()) dropItems.map { it.id } else UiUsageConstants.dropItems
    }
    val maintainable = cell.id in itemIds
    val existing = cell.plan
    val listedStageCodes = remember(stageGroups) {
        stageGroups.flatMap { group -> group.stages.map { it.code } }.toSet()
    }

    var draft by remember(cell.id, existing?.plan) {
        mutableStateOf(
            existing?.plan ?: DepotMaintainPlan(
                dropId = cell.id,
                // 默认保到现在的数量：不抬高目标就不会凭空多刷
                dropCount = cell.count.coerceAtLeast(1),
            ),
        )
    }

    // 关卡列表里选「自定义关卡」才放出输入框；已有计划用的是列表外的代码时默认就是它。
    // 只按 cell.id 初始化：这是用户意图，不能被关卡表的热更新重置掉。
    var customStage by remember(cell.id) {
        mutableStateOf(
            existing?.plan?.stage?.let { it.isNotBlank() && it !in listedStageCodes } ?: false,
        )
    }

    // 数字框被清空时（中间态）不要静默沿用上一个值，直接禁掉保存
    var targetBlank by remember(cell.id) { mutableStateOf(false) }

    // 面板收起来之后才落库：先滑下去再改数据，动画期间不会看到标题/按钮跳变
    var closing by remember { mutableStateOf(false) }
    val closeWithAnimation: (() -> Unit) -> Unit = { action ->
        if (!closing) {
            closing = true
            scope.launch {
                try {
                    sheetState.hide()
                } finally {
                    onDismiss()
                    action()
                }
            }
        }
    }

    // 目标库存跟着草稿实时算，改数字时右上的「已够/缺多少」立刻跟着变
    val draftTarget = draft.dropCount.coerceIn(1, MAX_TARGET_INVENTORY)
    val short = draftTarget > cell.count

    // 玻璃背景下面板会透出底下的库存网格，这里换回不透明配色
    OpaqueTheme {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(
                        start = MaaDesignTokens.Spacing.lg,
                        end = MaaDesignTokens.Spacing.lg,
                        bottom = MaaDesignTokens.Spacing.lg,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.md),
                ) {
                    ItemIcon(
                        itemId = cell.id,
                        contentDescription = cell.name,
                        size = 40.dp,
                        loader = iconLoader,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cell.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (maintainable) {
                                // 与格子上的「目标/当前」同一个顺序
                                stringResource(
                                    R.string.depot_inventory_maintain_progress,
                                    draftTarget,
                                    cell.count,
                                )
                            } else {
                                stringResource(R.string.depot_inventory_plan_current, cell.count)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // 已够/缺多少贴在面板最右，两行高的大字，绿=够 红=缺
                    if (maintainable) {
                        Text(
                            text = if (short) {
                                stringResource(
                                    R.string.depot_inventory_maintain_need,
                                    draftTarget - cell.count,
                                )
                            } else {
                                stringResource(R.string.depot_inventory_maintain_enough)
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (short) {
                                MaterialTheme.colorScheme.error
                            } else {
                                LogColorRole.SUCCESS.themedColor()
                            },
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            // 无约束的话 10 位缺口会先把左边的物品名挤没
                            modifier = Modifier.widthIn(max = 132.dp),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (!maintainable) {
                    Text(
                        text = stringResource(R.string.depot_inventory_plan_unsupported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    SectionHeader(
                        title = if (existing == null) {
                            stringResource(R.string.depot_inventory_plan_new)
                        } else {
                            stringResource(R.string.depot_inventory_maintain_title)
                        },
                    )

                    SheetNumericField(
                        value = draft.dropCount,
                        onValueChange = { draft = draft.copy(dropCount = it) },
                        onBlankChange = { targetBlank = it },
                        label = stringResource(R.string.panel_depot_target_inventory),
                        minimum = 1,
                        maximum = MAX_TARGET_INVENTORY,
                    )

                    if (stageGroups.isNotEmpty()) {
                        GroupedStageButtonGroup(
                            label = stringResource(R.string.depot_inventory_plan_stage_pick),
                            selectedValue = draft.stage,
                            stageGroups = stageGroups,
                            onItemSelected = {
                                draft = draft.copy(stage = it)
                                customStage = false
                            },
                            customLabel = stringResource(R.string.depot_inventory_plan_stage_custom),
                            customSelected = customStage,
                            onCustomSelected = { customStage = true },
                        )
                    }

                    // 选了「自定义关卡」才在列表下方放出输入框（关卡资源还没加载时只能手输）
                    if (customStage || stageGroups.isEmpty()) {
                        SheetStageField(
                            value = draft.stage,
                            onValueChange = { draft = draft.copy(stage = it) },
                            label = stringResource(R.string.depot_inventory_plan_stage),
                            placeholder = stringResource(
                                R.string.panel_fight_primary_stage_placeholder,
                            ),
                            stageCodes = stageCodes,
                        )
                    }

                    existing?.let { PlanOutcomeHint(it) }

                    // 手输的关卡码可能写错或今天没开，提前说一声，别等跑起来才发现被跳过
                    if (draft.stage.isNotBlank() && !activityManager.isStageOpen(draft.stage)) {
                        Text(
                            text = stringResource(
                                R.string.depot_inventory_maintain_stage_closed,
                                draft.stage,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (!context.nodeEnabled) {
                        Text(
                            text = stringResource(R.string.depot_inventory_plan_node_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm),
                    ) {
                        if (existing != null) {
                            OutlinedButton(
                                onClick = { closeWithAnimation(onRemove) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Text(stringResource(R.string.depot_inventory_plan_remove))
                            }
                        }
                        Button(
                            onClick = {
                                closeWithAnimation {
                                    onSave(
                                        draft.copy(
                                            dropCount = draftTarget,
                                            // 点「保存」时输入框可能还带着焦点，别名在这里兜一次
                                            stage = StageAliasMapper
                                                .mapToStageCode(draft.stage, stageCodes),
                                        ),
                                    )
                                }
                            },
                            // 目标框空着就别让保存，免得把上一次的数字当成新目标写进去
                            enabled = !targetBlank,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(stringResource(R.string.depot_inventory_plan_save))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 面板里的关卡输入框：输入即回写，失焦时把别名（龙门币/经验…）落成关卡代码。
 *
 * 不用 [StageInputField]：那个只在失焦时回调，用户敲完关卡码直接点「保存」时，
 * 值还没提交上去，保存的就还是旧关卡（数字框同理）。
 */
@Composable
private fun SheetStageField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    stageCodes: List<String>,
    modifier: Modifier = Modifier,
) {
    var convertedCode by remember { mutableStateOf("") }
    ITextFieldWithFocus(
        value = value,
        onValueChange = { raw ->
            onValueChange(raw)
            val mapped = StageAliasMapper.mapToStageCode(raw, stageCodes)
            // 映射结果和「只转大写」不同才说明命中了别名，这时才提示
            if (raw.isNotBlank() && mapped != raw.uppercase()) {
                convertedCode = mapped
            } else {
                convertedCode = ""
            }
        },
        onFocusLost = {
            if (value.isNotBlank()) {
                val mapped = StageAliasMapper.mapToStageCode(value, stageCodes)
                if (mapped != value) onValueChange(mapped)
                convertedCode = ""
            }
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = true,
        supportingText = if (convertedCode.isNotEmpty()) {
            {
                Text(
                    text = stringResource(R.string.panel_fight_converted_prefix, convertedCode),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
    )
}

/**
 * 面板里的数字输入框。
 *
 * 不用 [INumericField]：那个只在失焦时回调，用户敲完数字直接点「保存」时，
 * 值还没提交上去，保存的就还是旧目标。这里输入即回写，失焦只做归一化。
 */
@Composable
private fun SheetNumericField(
    value: Int,
    onValueChange: (Int) -> Unit,
    onBlankChange: (Boolean) -> Unit,
    label: String,
    minimum: Int,
    maximum: Int,
) {
    var text by remember { mutableStateOf(value.toString()) }
    ITextFieldWithFocus(
        value = text,
        onValueChange = { raw ->
            // 中间态（空串、前导 0）先留在框里，别急着改写成最小值
            if (raw.isEmpty() || raw.toIntOrNull() != null) {
                text = raw
                onBlankChange(raw.isEmpty())
                raw.toIntOrNull()?.let(onValueChange)
            }
        },
        onFocusLost = {
            val normalized = (text.toIntOrNull() ?: value).coerceIn(minimum, maximum)
            text = normalized.toString()
            onBlankChange(false)
            onValueChange(normalized)
        },
        modifier = Modifier.fillMaxWidth(),
        label = label,
        inputFilter = { it.isEmpty() || it.toIntOrNull() != null },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        inputType = InputType.TYPE_CLASS_NUMBER,
    )
}

/** 已保存计划里那些「配了也跑不起来」的原因，与运行日志同一套口径。 */
@Composable
private fun PlanOutcomeHint(plan: DepotMaintainPlanUi) {
    val text = when (plan.outcome) {
        DepotPlanOutcome.NoItem -> stringResource(R.string.depot_inventory_maintain_no_item)
        DepotPlanOutcome.ZeroTarget -> stringResource(R.string.depot_inventory_maintain_zero_target)
        DepotPlanOutcome.StageRequired ->
            stringResource(R.string.depot_inventory_maintain_stage_required)

        DepotPlanOutcome.StageClosed ->
            stringResource(R.string.depot_inventory_maintain_stage_closed, plan.plan.stage)

        // 能跑的计划，缺口由 PlanDraftProgress 按草稿实时算
        DepotPlanOutcome.Enough, DepotPlanOutcome.Runnable -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

// ============================== 导出 ==============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepotInventoryExportBottomSheet(
    profileId: String,
    titleLabel: String,
    exportScope: CoroutineScope,
    onDismiss: () -> Unit,
    viewModel: DepotInventoryViewModel,
    exporter: ToolboxFileExporter,
    exportLabels: OperBoxExportLabels,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val failedMsg = stringResource(R.string.toolbox_export_file_failed)
    var section by remember { mutableStateOf<ExportSection?>(null) }
    var hideProfileLabel by remember { mutableStateOf(false) }

    // 渲染放在页面的 scope 上：面板一收就取消的话，几秒的渲染会白跑一趟还没有任何提示
    val renderAndExport: (String, suspend () -> ByteArray?) -> Unit = { prefix, render ->
        exportScope.launch {
            val bytes = runCatching { render() }.getOrNull()
            if (bytes != null) {
                exporter.exportBytes(prefix, bytes, ToolboxExportFileType.PNG)
            } else {
                Toast.makeText(context, failedMsg, Toast.LENGTH_SHORT).show()
            }
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = MaaDesignTokens.Spacing.sm),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.depot_inventory_export_title, titleLabel),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaaDesignTokens.Spacing.lg,
                            vertical = MaaDesignTokens.Spacing.md,
                        ),
                )
                // 进了二级（仓库/干员）就只能整片关掉重来，给一条回一级的路
                if (section != null) {
                    IconButton(
                        onClick = { section = null },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = MaaDesignTokens.Spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accessibility_navigation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 一级 ↔ 二级横向推入推出，动效与页面内的 二级↔三级 一致；
            // 两级高度不同，交给 SizeTransform 收放，否则面板会在切换瞬间跳一下
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    val forward = initialState == null
                    val spec = tween<IntOffset>(220, easing = MaaMotion.Emphasized)
                    val enter = slideInHorizontally(animationSpec = spec) {
                        if (forward) it / 5 else -it / 5
                    } + fadeIn(tween(200))
                    val exit = slideOutHorizontally(animationSpec = spec) {
                        if (forward) -it / 5 else it / 5
                    } + fadeOut(tween(160))
                    (enter togetherWith exit).using(SizeTransform())
                },
                label = "depot-export-section",
            ) { current ->
                Column {
                    if (current != null) {
                        HideProfileLabelRow(
                            checked = hideProfileLabel,
                            onCheckedChange = { hideProfileLabel = it },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = MaaDesignTokens.Spacing.lg,
                                vertical = MaaDesignTokens.Spacing.xs,
                            ),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    when (current) {
                        null -> {
                            SheetItem(
                                text = stringResource(R.string.depot_inventory_export_depot),
                                icon = Icons.Default.Inventory2,
                                onClick = { section = ExportSection.DEPOT },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = MaaDesignTokens.Spacing.lg),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            SheetItem(
                                text = stringResource(R.string.depot_inventory_export_operbox),
                                icon = Icons.Default.Person,
                                onClick = { section = ExportSection.OPERBOX },
                            )
                        }

                        ExportSection.DEPOT -> {
                            SheetItem(
                                text = stringResource(R.string.depot_inventory_export_arkplanner),
                                icon = Icons.Default.Share,
                                onClick = {
                                    exporter.export(
                                        "depot_arkplanner",
                                        viewModel.exportDepotArkPlanner(profileId),
                                        ToolboxExportFileType.JSON,
                                    )
                                    onDismiss()
                                },
                            )
                            SheetItem(
                                text = stringResource(R.string.depot_inventory_export_lolicon),
                                icon = Icons.Default.Share,
                                onClick = {
                                    exporter.export(
                                        "depot_lolicon",
                                        viewModel.exportDepotLolicon(profileId),
                                        ToolboxExportFileType.JSON,
                                    )
                                    onDismiss()
                                },
                            )
                            SheetItem(
                                text = stringResource(R.string.depot_inventory_export_image),
                                icon = Icons.Default.Image,
                                onClick = {
                                    renderAndExport("depot") {
                                        viewModel.renderDepotPng(
                                            profileId = profileId,
                                            hideProfileLabel = hideProfileLabel,
                                            titleLabel = titleLabel,
                                        )
                                    }
                                },
                            )
                        }

                        ExportSection.OPERBOX -> {
                            SheetItem(
                                text = "JSON",
                                icon = Icons.Default.Share,
                                onClick = {
                                    exporter.export(
                                        "operbox",
                                        viewModel.exportOperBoxJson(profileId),
                                        ToolboxExportFileType.JSON,
                                    )
                                    onDismiss()
                                },
                            )
                            SheetItem(
                                text = "Markdown",
                                icon = Icons.Default.Share,
                                onClick = {
                                    exporter.export(
                                        "operbox",
                                        viewModel.exportOperBoxMarkdown(profileId, exportLabels),
                                        ToolboxExportFileType.MARKDOWN,
                                    )
                                    onDismiss()
                                },
                            )
                            SheetItem(
                                text = "CSV",
                                icon = Icons.Default.Share,
                                onClick = {
                                    exporter.export(
                                        "operbox",
                                        viewModel.exportOperBoxCsv(profileId, exportLabels),
                                        ToolboxExportFileType.CSV,
                                    )
                                    onDismiss()
                                },
                            )
                            SheetItem(
                                text = stringResource(R.string.depot_inventory_export_image),
                                icon = Icons.Default.Image,
                                onClick = {
                                    renderAndExport("operbox") {
                                        viewModel.renderOperBoxPng(
                                            profileId = profileId,
                                            hideProfileLabel = hideProfileLabel,
                                            titleLabel = titleLabel,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(MaaDesignTokens.Spacing.sm))
        }
    }
}

private enum class ExportSection { DEPOT, OPERBOX }

/** 二级菜单共用的一行：PNG 图头要不要写配置档名。 */
@Composable
private fun HideProfileLabelRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaaDesignTokens.Spacing.lg,
                vertical = MaaDesignTokens.Spacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = MaaDesignTokens.Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.depot_inventory_export_hide_profile),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.depot_inventory_export_hide_profile_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SheetItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
