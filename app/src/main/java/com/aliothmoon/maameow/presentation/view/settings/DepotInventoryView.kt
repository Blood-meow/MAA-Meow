package com.aliothmoon.maameow.presentation.view.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
import com.aliothmoon.maameow.domain.service.ToolboxExportFileType
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.view.panel.OperatorRow
import com.aliothmoon.maameow.presentation.view.panel.ToolboxFileExporter
import com.aliothmoon.maameow.presentation.view.panel.depot.DepotPlanFields
import com.aliothmoon.maameow.presentation.view.panel.depot.MAX_TARGET_INVENTORY
import com.aliothmoon.maameow.presentation.view.panel.depot.allStageCodes
import com.aliothmoon.maameow.presentation.view.panel.depot.rememberDepotItemIds
import com.aliothmoon.maameow.presentation.view.panel.depot.rememberDepotStageGroups
import com.aliothmoon.maameow.presentation.view.panel.rememberOperBoxExportLabels
import com.aliothmoon.maameow.presentation.view.panel.rememberSafToolboxFileExporter
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryCellUi
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryViewModel
import com.aliothmoon.maameow.presentation.viewmodel.DepotMaintainPlanUi
import com.aliothmoon.maameow.presentation.viewmodel.DepotPlanContext
import com.aliothmoon.maameow.presentation.viewmodel.DepotPngLabels
import com.aliothmoon.maameow.presentation.viewmodel.DepotProfileRow
import com.aliothmoon.maameow.presentation.viewmodel.OperBoxPngLabels
import com.aliothmoon.maameow.presentation.viewmodel.groupForDisplay
import com.aliothmoon.maameow.theme.LocalReduceMotion
import com.aliothmoon.maameow.theme.MaaAnimatedVisibility
import com.aliothmoon.maameow.theme.MaaDesignTokens
import com.aliothmoon.maameow.theme.MaaMotion
import com.aliothmoon.maameow.theme.OpaqueTheme
import com.aliothmoon.maameow.theme.themedColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/** 库存为 0 的格子压到多淡；太淡就看不出是什么材料了 */
private const val EMPTY_CELL_ALPHA = 0.32f

/**
 * 格子之间的竖缝。挂到每个条目自己身上，而不是交给网格的 verticalArrangement：
 * 「库存为 0」那段收起时要让格子真的缩到 0 高，挂在条目外面的间距缩不掉，
 * 二十来行攒下来就是一百多 dp 的空档。
 */
private val CellGap = 6.dp

/** 「库存为 0」那段标题行的 key：展开时要按它把这一段顶回眼前 */
private const val EMPTY_BREAK_KEY = "empty-break"

/** 展开时逐格错开多少毫秒；同一排差几毫秒，看着还是一起长，排与排之间才拉得开 */
private const val EMPTY_REVEAL_STEP_MS = 6L

/** 错开的上限：再往下的格子都在屏幕外，没必要排到几百毫秒之后 */
private const val EMPTY_REVEAL_MAX_MS = 150L

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
                        onDragCancel = {
                            dragging = false
                            // 取消也要收敛回去，否则卡片停在半开位置
                            dragOffsetX = if (dragOffsetX < -actionWidthPx / 2f) {
                                -actionWidthPx
                            } else {
                                0f
                            }
                        },
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
    // 存格子的 key 而不是物品 id：同一物品配了多条计划时会有多格，按 id 找只会拿到第一格
    var planSheetCellKey by remember { mutableStateOf<String?>(null) }

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
                onCellClick = { planSheetCellKey = it.key },
            )
        }
    }

    planSheetCellKey?.let { cellKey ->
        cells.firstOrNull { it.key == cellKey }?.let { cell ->
            DepotMaintainPlanSheet(
                cell = cell,
                // 已有计划就跟着它所属节点的开关走，新计划才用「第一个库存保持节点」的状态
                context = cell.plan?.node ?: planContext,
                onDismiss = { planSheetCellKey = null },
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
    // 同理，「库存为 0」那段的折叠状态也放外面；默认收起，几十个空格子不该占着屏
    var emptyCollapsed by rememberSaveable { mutableStateOf(true) }
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
                    emptyCollapsed = emptyCollapsed,
                    onToggleEmpty = { emptyCollapsed = !emptyCollapsed },
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
 * 分三段：有库存 → 未集齐 → 库存为 0。
 * 未集齐单独成组，免得缺货的东西埋在几十个已够的物品里找不到；
 * 库里一条记录都没有的材料也补齐格子（图标压淡），一眼看得出「这段都是没打过的」。
 *
 * 最后一段默认折叠：它通常占掉大半个网格，而「没打过的材料」本来就是拿来对账用的。
 * 标题上的数量一直显示，收起时也不会让人以为格子丢了。
 * 展开时这一段是从标题行底下长出来的（见 [RevealCell]），不是整片凭空出现。
 *
 * @param emptyCollapsed 「库存为 0」那段是否收起
 * @param onToggleEmpty 点那段的标题行时切折叠
 */
@Composable
private fun DepotItemsPage(
    cells: List<DepotInventoryCellUi>,
    iconLoader: ItemIconLoader,
    onCellClick: (DepotInventoryCellUi) -> Unit,
    emptyCollapsed: Boolean,
    onToggleEmpty: () -> Unit,
) {
    if (cells.isEmpty()) {
        DetailEmptyText(R.string.depot_inventory_empty_items)
        return
    }
    // 一次遍历分组，且只在 cells 真变了才重算（否则每次重组都白跑两遍 filter）
    val groups = remember(cells) { cells.groupForDisplay() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // 收着的时候标题行就是列表的最后一行，通常正贴着屏幕下沿 ——
    // 这时展开出来的格子全在屏幕外，眼睛只能看到箭头翻了个面，像是没动效。
    // 下面留不出一整排的位置就先记下标题行的位置，展开后把它顶回列表头，
    // 让淡入发生在眼前；本来就留得下就不动，免得平白滚一下。
    val oneRow = with(LocalDensity.current) { 108.dp.roundToPx() }
    val toggleEmpty = {
        val header = gridState.layoutInfo.visibleItemsInfo
            .lastOrNull { it.key == EMPTY_BREAK_KEY }
        val room = header?.let {
            gridState.layoutInfo.viewportEndOffset - (it.offset.y + it.size.height)
        }
        val headerIndex = header?.index
        val expanding = emptyCollapsed
        onToggleEmpty()
        if (expanding && headerIndex != null && room != null && room < oneRow) {
            scope.launch { gridState.animateScrollToItem(headerIndex) }
        }
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 92.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaaDesignTokens.Spacing.md),
        contentPadding = PaddingValues(top = 6.dp, bottom = MaaDesignTokens.Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        items(groups.stocked, key = { "stocked-${it.key}" }) { cell ->
            InventoryItemCell(cell = cell, iconLoader = iconLoader, onClick = onCellClick)
        }
        if (groups.unmet.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "unmet-break") {
                CellSectionBreak(
                    textRes = R.string.depot_inventory_section_unmet,
                    count = groups.unmet.size,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            items(groups.unmet, key = { "unmet-${it.key}" }) { cell ->
                InventoryItemCell(cell = cell, iconLoader = iconLoader, onClick = onCellClick)
            }
        }
        if (groups.empty.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = EMPTY_BREAK_KEY) {
                CellSectionBreak(
                    textRes = R.string.depot_inventory_section_empty,
                    count = groups.empty.size,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    collapsed = emptyCollapsed,
                    onToggle = toggleEmpty,
                )
            }
            // 收着的时候这些格子也留在列表里，只是高度为 0 ——
            // 这样展开、收起两边都能做「从标题行底下长出来」，
            // 而不是整片凭空出现、或者整片瞬间消失
            itemsIndexed(groups.empty, key = { _, it -> "empty-${it.key}" }) { index, cell ->
                RevealCell(visible = !emptyCollapsed, index = index) {
                    InventoryItemCell(
                        cell = cell,
                        iconLoader = iconLoader,
                        onClick = onCellClick,
                        dimmed = true,
                    )
                }
            }
        }
    }
}

/**
 * 「库存为 0」的一格：展开时从标题行底下长出来。
 *
 * 高度从 0 撑开、内容自上而下露出来（[MaaAnimatedVisibility] 就是全 App 那个展开 idiom），
 * 再按序错开几毫秒，整段看着是往外摊开的，而不是整片同时亮一下。
 *
 * 收起时格子仍留在列表里（高度 0），所以两个方向都有动效、也不会留下空档；
 * 展开之后再滚进来的格子是「第一次组合就可见」，直接到位，不会再弹一次。
 */
@Composable
private fun RevealCell(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    // 初值取当前可见性：第一次组合就可见的格子（展开后滚动进来的）不该补一次动效
    var shown by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible && !shown && !reduceMotion) {
            delay((index * EMPTY_REVEAL_STEP_MS).coerceAtMost(EMPTY_REVEAL_MAX_MS))
        }
        shown = visible
    }
    MaaAnimatedVisibility(
        visible = shown,
        label = "depot-empty-cell",
        content = { content() },
    )
}

/**
 * 分段行：一条横线加组标题，把网格断成几段。
 *
 * [onToggle] 非空时整行可点，整行最右多一个跟着 [collapsed] 转 180° 的箭头。
 * 折叠语义跟 CollapsibleSection 保持一致：收起时箭头朝下，
 * 播报文案复用 common_expand / common_collapse 那一对。
 */
@Composable
private fun CellSectionBreak(
    textRes: Int,
    count: Int,
    color: Color,
    collapsed: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val expandLabel = stringResource(R.string.common_expand)
    val collapseLabel = stringResource(R.string.common_collapse)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaaDesignTokens.Spacing.sm, bottom = CellGap),
    ) {
        HorizontalDivider(color = color.copy(alpha = 0.4f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onToggle == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            role = Role.Button,
                            onClickLabel = if (collapsed) expandLabel else collapseLabel,
                            onClick = onToggle,
                        )
                    }
                )
                .padding(
                    top = MaaDesignTokens.Spacing.xs,
                    bottom = 2.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(textRes, count),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
            if (onToggle != null) {
                // 箭头贴到整行最右：它是「这行能展开」的提示，跟着标题走会被当成标题的一部分
                Spacer(modifier = Modifier.weight(1f))
                val arrowRotation by animateFloatAsState(
                    targetValue = if (collapsed) 0f else 180f,
                    animationSpec = MaaMotion.spec(LocalReduceMotion.current, MaaMotion.Fast),
                    label = "depot-section-arrow",
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (collapsed) expandLabel else collapseLabel,
                    tint = color,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(arrowRotation),
                )
            }
        }
    }
}

/**
 * 一格库存：图标下面是「目标/当前」，未集齐标红、已集齐标绿。
 *
 * 库存和库存保持是同一件事，所以不分成两个区块：没配计划的物品显示纯数量，
 * 配了计划的直接显示目标与当前，红色就代表还差。
 *
 * @param dimmed 库存为 0 的格子压淡，但仍然可点——点它就是给这个材料加计划
 * @param modifier 交给调用方挂 [RevealCell] 之类的条目级动效
 */
@Composable
private fun InventoryItemCell(
    cell: DepotInventoryCellUi,
    iconLoader: ItemIconLoader,
    onClick: (DepotInventoryCellUi) -> Unit,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            // 竖缝挂在自己身上，收起时才缩得干净（见 CellGap）
            .padding(bottom = CellGap)
            .fillMaxWidth()
            .alpha(if (dimmed) EMPTY_CELL_ALPHA else 1f)
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
 * 面板的三种形态。声明顺序就是「越往后越深」，切换方向按它算：
 * 摘要卡 → 编辑表单是往前推，反着来（目前没有这条路）就往回推。
 */
private enum class DepotPlanSheetMode { UNSUPPORTED, SUMMARY, EDITOR }

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

    // 关卡表与材料表都是全 App 同一份，别再各写一遍过滤逻辑（见 DepotPlanFields）
    val stageGroups = rememberDepotStageGroups(activityManager)
    val stageCodes = remember(stageGroups) { stageGroups.allStageCodes() }
    val itemIds = rememberDepotItemIds(itemHelper)
    val existing = cell.plan
    // 已经有计划的物品永远可编辑：老数据里可能留着识别集合之外的物品（比如赤金），
    // 只按列表放行的话那条计划在这一页既改不了也删不掉
    val maintainable = existing != null || cell.id in itemIds

    // 没计划的物品不直接铺表单：先出一张只讲「是什么、现在有多少」的摘要卡。
    // 直接进表单会凭空算出一个「目标 144 / 当前 144 · 已够」的计划，
    // 看着像已经配好了，其实什么都没配。
    var creating by remember(cell.id) { mutableStateOf(false) }
    val mode = when {
        !maintainable -> DepotPlanSheetMode.UNSUPPORTED
        existing != null || creating -> DepotPlanSheetMode.EDITOR
        else -> DepotPlanSheetMode.SUMMARY
    }
    val reduceMotion = LocalReduceMotion.current

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
            existing?.plan?.stage?.let { it.isNotBlank() && it !in stageCodes } ?: false,
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
            // 摘要卡 → 编辑表单是同一张面板换形态：横向推入推出，高度交给 SizeTransform 收放，
            // 参数与本文件导出面板的 一级↔二级 完全一致；系统关掉动画时直接换
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    if (reduceMotion) {
                        (EnterTransition.None togetherWith ExitTransition.None).using(null)
                    } else {
                        val forward = targetState.ordinal > initialState.ordinal
                        val spec = tween<IntOffset>(MaaMotion.Medium, easing = MaaMotion.Emphasized)
                        // 推 1/3 屏：面板里换的是「一整屏设置」，只挪一点点会被当成直接换掉了
                        val enter = slideInHorizontally(animationSpec = spec) {
                            if (forward) it / 3 else -it / 3
                        } + fadeIn(tween(durationMillis = MaaMotion.Fast, easing = MaaMotion.Linear))
                        val exit = slideOutHorizontally(animationSpec = spec) {
                            if (forward) -it / 3 else it / 3
                        } + fadeOut(tween(durationMillis = MaaMotion.Fast, easing = MaaMotion.Linear))
                        (enter togetherWith exit).using(SizeTransform())
                    }
                },
                label = "depot-plan-mode",
                modifier = Modifier.fillMaxWidth(),
            ) { current ->
                val editing = current == DepotPlanSheetMode.EDITOR
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
                                text = if (editing) {
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
                        // 已够/缺多少贴在面板最右，两行高的大字，绿=够 红=缺。
                        // 只在编辑态出现：没有计划时谈「够不够」是空话
                        if (editing) {
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

                    if (current == DepotPlanSheetMode.UNSUPPORTED) {
                        Text(
                            text = stringResource(R.string.depot_inventory_plan_unsupported),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (current == DepotPlanSheetMode.SUMMARY) {
                        // 摘要卡到此为止：设置留到下一屏，按钮是卡片唯一的出口
                        Button(
                            onClick = { creating = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(stringResource(R.string.depot_inventory_plan_new))
                        }
                    } else {
                        SectionHeader(
                            title = if (existing == null) {
                                stringResource(R.string.depot_inventory_plan_new)
                            } else {
                                stringResource(R.string.depot_inventory_maintain_title)
                            },
                        )

                        // 字段本体与任务配置页共用一份，见 DepotPlanFields
                        DepotPlanFields(
                            plan = draft,
                            onPlanChange = { draft = it },
                            stageGroups = stageGroups,
                            stageCodes = stageCodes,
                            customStageCode = customStage,
                            onCustomStageSelected = { customStage = true },
                            showStageListWithCustom = true,
                            onStageSelected = {
                                draft = draft.copy(stage = it)
                                customStage = false
                            },
                            onTargetBlankChange = { targetBlank = it },
                        )

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
    // 图里的文案在这里解析好再传进去：ViewModel 不碰 Context
    val depotPngLabels = DepotPngLabels(
        itemsCountFormat = stringResource(R.string.depot_inventory_png_items),
        empty = stringResource(R.string.depot_inventory_png_empty),
    )
    val operBoxPngLabels = OperBoxPngLabels(
        summaryFormat = stringResource(R.string.depot_inventory_png_oper_summary),
        empty = stringResource(R.string.depot_inventory_png_empty),
        operMetaFormat = stringResource(R.string.depot_inventory_png_oper_meta),
    )
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
                                            labels = depotPngLabels,
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
                                            labels = operBoxPngLabels,
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
