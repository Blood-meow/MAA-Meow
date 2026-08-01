package com.aliothmoon.maameow.presentation.view.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportLabels
import com.aliothmoon.maameow.data.repository.DepotAccountSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxSnapshot
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import com.aliothmoon.maameow.domain.service.ToolboxExportFileType
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.view.panel.DepotItemCell
import com.aliothmoon.maameow.presentation.view.panel.OperatorRow
import com.aliothmoon.maameow.presentation.view.panel.ToolboxFileExporter
import com.aliothmoon.maameow.presentation.view.panel.rememberSafToolboxFileExporter
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryItemUi
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryViewModel
import com.aliothmoon.maameow.presentation.viewmodel.drawSummary
import com.aliothmoon.maameow.presentation.viewmodel.mergeAccountRows
import com.aliothmoon.maameow.theme.MaaAnimations
import com.aliothmoon.maameow.theme.MaaDesignTokens
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun DepotInventoryView(
    navController: NavController,
    viewModel: DepotInventoryViewModel = koinViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val operBoxAccounts by viewModel.operBoxAccounts.collectAsStateWithLifecycle()
    val selectedAccountTag by viewModel.selectedAccountTag.collectAsStateWithLifecycle()
    val accountRows = remember(accounts, operBoxAccounts) {
        mergeAccountRows(accounts, operBoxAccounts)
    }
    val exporter = rememberSafToolboxFileExporter()
    val exportLabels = rememberOperBoxExportLabels()
    var exportAccountTag by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTag by remember { mutableStateOf<String?>(null) }

    // 进入页面强制刷新，避免小游戏更新后列表仍是旧快照
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    BackHandler(enabled = selectedAccountTag != null) { viewModel.clearSelection() }

    AnimatedContent(
        targetState = selectedAccountTag,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState != null) {
                MaaAnimations.sharedAxisForwardEnter togetherWith MaaAnimations.sharedAxisForwardExit
            } else {
                MaaAnimations.sharedAxisPopEnter togetherWith MaaAnimations.sharedAxisPopExit
            }
        },
        label = "depot-account-navigation",
    ) { accountTag ->
        if (accountTag == null) {
            DepotAccountListView(
                accounts = accountRows,
                onBack = { navController.navigateUp() },
                onRefresh = { viewModel.refresh() },
                onAccountClick = { viewModel.selectAccount(it.accountTag) },
                onDelete = { pendingDeleteTag = it },
                onExport = { exportAccountTag = it },
            )
        } else {
            DepotAccountDetailView(
                accountTag = accountTag,
                items = viewModel.itemsForAccount(accountTag),
                operBox = operBoxAccounts.firstOrNull { it.accountTag == accountTag }?.snapshot
                    ?: OperBoxSnapshot(),
                onBack = { viewModel.clearSelection() },
            )
        }
    }

    exportAccountTag?.let { tag ->
        DepotInventoryExportBottomSheet(
            accountTag = tag,
            onDismiss = { exportAccountTag = null },
            viewModel = viewModel,
            exporter = exporter,
            exportLabels = exportLabels,
        )
    }

    pendingDeleteTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTag = null },
            title = { Text(stringResource(R.string.depot_inventory_delete_title)) },
            text = { Text(stringResource(R.string.depot_inventory_delete_message, tag)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(tag)
                        pendingDeleteTag = null
                    },
                ) {
                    Text(stringResource(R.string.depot_inventory_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTag = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DepotAccountListView(
    accounts: List<DepotAccountSnapshot>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAccountClick: (DepotAccountSnapshot) -> Unit,
    onDelete: (String) -> Unit,
    onExport: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.depot_inventory_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.depot_inventory_refresh),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (accounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.depot_inventory_empty_accounts),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    items(accounts, key = { it.accountTag }) { account ->
                        SwipeRevealAccountCard(
                            account = account,
                            onClick = { onAccountClick(account) },
                            onDelete = { onDelete(account.accountTag) },
                            onExport = { onExport(account.accountTag) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeRevealAccountCard(
    account: DepotAccountSnapshot,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 144.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    // 用卡片实测高度，保证分享/删除与卡片完全等高（之前 matchParentSize 那版的高度手感）
    var cardHeightPx by remember { mutableIntStateOf(0) }
    val revealPx = (-offsetX.value).coerceIn(0f, actionWidthPx)
    val revealDp = with(density) { revealPx.toDp() }
    val cardHeightDp = with(density) { cardHeightPx.toDp() }
    val revealed = revealPx > 8f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
    ) {
        // 操作钮只放在右侧「露出条带」里：卡片半透明也不会透出整块按钮
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
                            .clickable(enabled = revealed, onClick = onDelete),
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
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(actionWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val target = if (offsetX.value < -actionWidthPx / 2f) -actionWidthPx else 0f
                                offsetX.animateTo(target, tween(180))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(-actionWidthPx, 0f)
                                offsetX.snapTo(next)
                            }
                        },
                    )
                }
                .clickable(onClick = {
                    if (offsetX.value < -8f) {
                        scope.launch { offsetX.animateTo(0f, tween(150)) }
                    } else {
                        onClick()
                    }
                }),
            // 保持半透明 surfaceVariant
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val draws = account.drawSummary()
            Column(
                modifier = Modifier.padding(MaaDesignTokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
            ) {
                Text(
                    text = account.accountTag,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.depot_inventory_available_draws,
                        draws.total,
                        draws.orundum,
                        draws.permits,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (account.snapshot.syncTimeMillis > 0L) {
                    Text(
                        text = stringResource(
                            R.string.depot_inventory_sync_time,
                            DateFormat.getDateTimeInstance().format(Date(account.snapshot.syncTimeMillis)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepotInventoryExportBottomSheet(
    accountTag: String,
    onDismiss: () -> Unit,
    viewModel: DepotInventoryViewModel,
    exporter: ToolboxFileExporter,
    exportLabels: OperBoxExportLabels,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf<ExportSection?>(null) }
    // 导出图片时可选：隐去 accountTag（official 后那串），只留时间与数据
    var hideAccountTag by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.depot_inventory_export_title, accountTag),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            // 仅在选了仓库/干员后显示：导出图片是否隐去账号标识
            if (section != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.depot_inventory_export_hide_account),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.depot_inventory_export_hide_account_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = hideAccountTag,
                        onCheckedChange = { hideAccountTag = it },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }

            when (section) {
                null -> {
                    SheetItem(
                        text = stringResource(R.string.depot_inventory_export_depot),
                        icon = Icons.Default.Inventory2,
                        onClick = { section = ExportSection.DEPOT },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
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
                                "depot_arkplanner_$accountTag",
                                viewModel.exportDepotArkPlanner(accountTag),
                                ToolboxExportFileType.JSON,
                            )
                            onDismiss()
                        },
                    )
                    SheetItem(
                        text = stringResource(R.string.depot_inventory_export_lolicon),
                        icon = Icons.Default.Share,
                        onClick = {
                            // lolicon 也是 JSON 文本
                            exporter.export(
                                "depot_lolicon_$accountTag",
                                viewModel.exportDepotLolicon(accountTag),
                                ToolboxExportFileType.JSON,
                            )
                            onDismiss()
                        },
                    )
                    SheetItem(
                        text = stringResource(R.string.depot_inventory_export_image),
                        icon = Icons.Default.Image,
                        onClick = {
                            scope.launch {
                                val bytes = viewModel.renderDepotPng(accountTag, hideAccountTag = hideAccountTag)
                                if (bytes != null) {
                                    exporter.exportBytes("depot_$accountTag", bytes, ToolboxExportFileType.PNG)
                                }
                                onDismiss()
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
                                "operbox_$accountTag",
                                viewModel.exportOperBoxJson(accountTag),
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
                                "operbox_$accountTag",
                                viewModel.exportOperBoxMarkdown(accountTag, exportLabels),
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
                                "operbox_$accountTag",
                                viewModel.exportOperBoxCsv(accountTag, exportLabels),
                                ToolboxExportFileType.CSV,
                            )
                            onDismiss()
                        },
                    )
                    SheetItem(
                        text = stringResource(R.string.depot_inventory_export_image),
                        icon = Icons.Default.Image,
                        onClick = {
                            scope.launch {
                                val bytes = viewModel.renderOperBoxPng(accountTag, hideAccountTag = hideAccountTag)
                                if (bytes != null) {
                                    exporter.exportBytes("operbox_$accountTag", bytes, ToolboxExportFileType.PNG)
                                }
                                onDismiss()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class ExportSection { DEPOT, OPERBOX }

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

@Composable
private fun rememberOperBoxExportLabels(): OperBoxExportLabels {
    val name = stringResource(R.string.operbox_export_header_name)
    val id = stringResource(R.string.operbox_export_header_id)
    val rarity = stringResource(R.string.operbox_export_header_rarity)
    val elite = stringResource(R.string.operbox_export_header_elite)
    val level = stringResource(R.string.operbox_export_header_level)
    val own = stringResource(R.string.operbox_export_header_own)
    val potential = stringResource(R.string.operbox_export_header_potential)
    val yes = stringResource(R.string.operbox_export_yes)
    val no = stringResource(R.string.operbox_export_no)
    return remember(name, id, rarity, elite, level, own, potential, yes, no) {
        OperBoxExportLabels(name, id, rarity, elite, level, own, potential, yes, no)
    }
}

@Composable
private fun DepotAccountDetailView(
    accountTag: String,
    items: List<DepotInventoryItemUi>,
    operBox: OperBoxSnapshot,
    onBack: () -> Unit,
    iconLoader: ItemIconLoader = koinInject(),
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val pageLabels = listOf(
        stringResource(R.string.depot_inventory_tab_items),
        stringResource(R.string.depot_inventory_tab_operators),
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.depot_inventory_detail_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = accountTag,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = MaaDesignTokens.Spacing.sm),
            )
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
                    0 -> DepotItemsPage(items = items, iconLoader = iconLoader)
                    else -> OperBoxPage(snapshot = operBox)
                }
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
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
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
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun DepotItemsPage(
    items: List<DepotInventoryItemUi>,
    iconLoader: ItemIconLoader,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DetailEmptyText(R.string.depot_inventory_empty_items)
            }
        } else {
            items(items, key = { it.id }) { item ->
                DepotItemCell(item.id, item.count, item.name, iconLoader)
            }
        }
    }
}

@Composable
private fun OperBoxPage(snapshot: OperBoxSnapshot) {
    var selectedTab by remember { mutableIntStateOf(0) }
    if (!snapshot.hasSynced) {
        DetailEmptyText(R.string.depot_inventory_empty_operators)
        return
    }
    val operators = if (selectedTab == 0) snapshot.owned else snapshot.notOwned
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { selectedTab = index }
                            .padding(vertical = 4.dp),
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 32.dp),
    )
}
