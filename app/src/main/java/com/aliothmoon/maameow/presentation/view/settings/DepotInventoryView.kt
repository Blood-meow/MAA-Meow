package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportLabels
import com.aliothmoon.maameow.data.repository.OperBoxSnapshot
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import com.aliothmoon.maameow.domain.service.ToolboxExportFileType
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.view.panel.OperatorRow
import com.aliothmoon.maameow.presentation.view.panel.ToolboxFileExporter
import com.aliothmoon.maameow.presentation.view.panel.rememberSafToolboxFileExporter
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryItemUi
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.DateFormat
import java.util.Date

@Composable
fun DepotInventoryView(
    navController: NavController,
    viewModel: DepotInventoryViewModel = koinViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val operBox by viewModel.operBoxSnapshot.collectAsStateWithLifecycle()
    val draws by viewModel.drawSummary.collectAsStateWithLifecycle()
    val depotSnap by viewModel.depotSnapshot.collectAsStateWithLifecycle()
    val exporter = rememberSafToolboxFileExporter()
    val exportLabels = rememberOperBoxExportLabels()
    var showExport by remember { mutableStateOf(false) }
    var pendingClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.depot_inventory_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    IconButton(onClick = { showExport = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.depot_inventory_export),
                        )
                    }
                    IconButton(onClick = { pendingClear = true }) {
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
            val hasAny = items.isNotEmpty() || operBox.hasSynced
            if (!hasAny) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.depot_inventory_empty_accounts),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = MaaDesignTokens.Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                ) {
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
                    if (depotSnap.syncTimeMillis > 0L) {
                        Text(
                            text = stringResource(
                                R.string.depot_inventory_sync_time,
                                DateFormat.getDateTimeInstance().format(Date(depotSnap.syncTimeMillis)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    InventoryDetailBody(items = items, operBox = operBox)
                }
            }
        }
    }

    if (showExport) {
        DepotInventoryExportBottomSheet(
            onDismiss = { showExport = false },
            viewModel = viewModel,
            exporter = exporter,
            exportLabels = exportLabels,
            titleLabel = stringResource(R.string.depot_inventory_title),
        )
    }

    if (pendingClear) {
        AlertDialog(
            onDismissRequest = { pendingClear = false },
            title = { Text(stringResource(R.string.depot_inventory_delete_title)) },
            text = { Text(stringResource(R.string.depot_inventory_delete_message_profile)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        pendingClear = false
                    },
                ) {
                    Text(
                        stringResource(R.string.depot_inventory_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun InventoryDetailBody(
    items: List<DepotInventoryItemUi>,
    operBox: OperBoxSnapshot,
    iconLoader: ItemIconLoader = koinInject(),
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val pageLabels = listOf(
        stringResource(R.string.depot_inventory_tab_items),
        stringResource(R.string.depot_inventory_tab_operators),
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
            .fillMaxSize(),
        userScrollEnabled = true,
    ) { page ->
        when (page) {
            0 -> DepotItemsPage(items = items, iconLoader = iconLoader)
            else -> OperBoxPage(snapshot = operBox)
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
                InventoryItemCell(item, iconLoader)
            }
        }
    }
}

@Composable
private fun InventoryItemCell(
    item: DepotInventoryItemUi,
    iconLoader: ItemIconLoader,
) {
    // 轻量本地格子，避免依赖 private DepotItemCell
    val iconState = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        item.id,
    ) {
        value = iconLoader.load(item.id)
    }
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val bitmap = iconState.value
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = item.name,
                    modifier = Modifier
                        .height(44.dp)
                        .fillMaxWidth(0.7f),
                )
            } else {
                Spacer(Modifier.height(44.dp))
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = "x${item.count}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
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
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
        items(operators, key = { it.id }) { op ->
            OperatorRow(op)
        }
    }
}

@Composable
private fun DetailEmptyText(resId: Int) {
    Text(
        text = stringResource(resId),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepotInventoryExportBottomSheet(
    onDismiss: () -> Unit,
    viewModel: DepotInventoryViewModel,
    exporter: ToolboxFileExporter,
    exportLabels: OperBoxExportLabels,
    titleLabel: String,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf<ExportSection?>(null) }
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
                text = stringResource(R.string.depot_inventory_export_title, titleLabel),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

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
                                "depot_arkplanner",
                                viewModel.exportDepotArkPlanner(),
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
                                viewModel.exportDepotLolicon(),
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
                                val bytes = viewModel.renderDepotPng(
                                    hideAccountTag = hideAccountTag,
                                    titleLabel = titleLabel,
                                )
                                if (bytes != null) {
                                    exporter.exportBytes("depot", bytes, ToolboxExportFileType.PNG)
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
                                "operbox",
                                viewModel.exportOperBoxJson(),
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
                                viewModel.exportOperBoxMarkdown(exportLabels),
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
                                viewModel.exportOperBoxCsv(exportLabels),
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
                                val bytes = viewModel.renderOperBoxPng(
                                    hideAccountTag = hideAccountTag,
                                    titleLabel = titleLabel,
                                )
                                if (bytes != null) {
                                    exporter.exportBytes("operbox", bytes, ToolboxExportFileType.PNG)
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
