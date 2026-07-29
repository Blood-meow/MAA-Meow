package com.aliothmoon.maameow.presentation.view.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.repository.DepotAccountSnapshot
import com.aliothmoon.maameow.data.repository.OperBoxSnapshot
import com.aliothmoon.maameow.data.resource.ItemIconLoader
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.view.panel.DepotItemCell
import com.aliothmoon.maameow.presentation.view.panel.OperatorRow
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
                onAccountClick = { viewModel.selectAccount(it.accountTag) },
            )
        } else {
            DepotAccountDetailView(
                accountTag = accountTag,
                items = viewModel.itemsForAccount(accountTag),
                operBox = operBoxAccounts.firstOrNull { it.accountTag == accountTag }?.snapshot ?: OperBoxSnapshot(),
                onBack = { viewModel.clearSelection() },
            )
        }
    }
}

@Composable
private fun DepotAccountListView(
    accounts: List<DepotAccountSnapshot>,
    onBack: () -> Unit,
    onAccountClick: (DepotAccountSnapshot) -> Unit,
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
                        DepotAccountCard(account = account, onClick = { onAccountClick(account) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DepotAccountCard(account: DepotAccountSnapshot, onClick: () -> Unit) {
    val draws = account.drawSummary()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
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
