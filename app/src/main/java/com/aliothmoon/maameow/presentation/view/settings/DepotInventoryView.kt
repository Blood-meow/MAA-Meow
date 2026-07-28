package com.aliothmoon.maameow.presentation.view.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryItemUi
import com.aliothmoon.maameow.presentation.viewmodel.DepotInventoryViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun DepotInventoryView(
    navController: NavController,
    viewModel: DepotInventoryViewModel = koinViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val selectedAccountTag by viewModel.selectedAccountTag.collectAsStateWithLifecycle()

    BackHandler(enabled = selectedAccountTag != null) { viewModel.clearSelection() }

    if (selectedAccountTag == null) {
        DepotAccountListView(
            accounts = accounts,
            onBack = { navController.navigateUp() },
            onAccountClick = { viewModel.selectAccount(it.accountTag) },
        )
    } else {
        DepotAccountDetailView(
            accountTag = selectedAccountTag.orEmpty(),
            items = viewModel.selectedItems(),
            onBack = { viewModel.clearSelection() },
        )
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
                    R.string.depot_inventory_account_summary,
                    account.itemKinds,
                    account.totalCount,
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
    onBack: () -> Unit,
) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = MaaDesignTokens.Spacing.listHorizontal,
                vertical = MaaDesignTokens.Spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
        ) {
            item {
                Text(
                    text = accountTag,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = MaaDesignTokens.Spacing.sm),
                )
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.depot_inventory_empty_items),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            } else {
                items(items, key = { it.id }) { item -> DepotItemRow(item) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun DepotItemRow(item: DepotInventoryItemUi) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaaDesignTokens.Spacing.md, vertical = MaaDesignTokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = item.count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
