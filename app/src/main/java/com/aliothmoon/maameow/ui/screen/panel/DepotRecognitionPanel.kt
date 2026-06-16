package com.aliothmoon.maameow.ui.screen.panel

import android.content.ClipData
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.toolbox.DepotItem
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.ui.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.utils.i18n.asString
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

@Composable
fun DepotRecognitionPanel(
    modifier: Modifier = Modifier,
    viewModel: ToolboxViewModel = koinInject(),
    itemHelper: ItemHelper = koinInject()
) {
    val items by viewModel.collector.depotItems.collectAsStateWithLifecycle()
    val itemMap by itemHelper.items.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val resolvedStatusMessage = statusMessage.asString()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copyPenguinToast = stringResource(R.string.panel_depot_copy_penguin)
    val copyToolboxToast = stringResource(R.string.panel_depot_copy_toolbox)

    if (items.isEmpty()) {
        DepotEmptyState(modifier, resolvedStatusMessage)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 80.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 统计信息 + 导出按钮
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MaaUiText(
                    text = stringResource(R.string.panel_depot_item_count, items.size),
                    style = ThemeTypography.bodySmall,
                    color = ThemeColors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val text = viewModel.exportDepotArkPlanner()
                                val entry = ClipData.newPlainText("label", text).toClipEntry()
                                clipboard.setClipEntry(entry)
                            }
                            Toast.makeText(context, copyPenguinToast, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        MaaUiText(
                            stringResource(R.string.panel_depot_export_penguin),
                            style = ThemeTypography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val text = viewModel.exportDepotLolicon()
                                val entry = ClipData.newPlainText("label", text).toClipEntry()
                                clipboard.setClipEntry(entry)
                            }
                            Toast.makeText(context, copyToolboxToast, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        MaaUiText(
                            stringResource(R.string.panel_depot_export_toolbox),
                            style = ThemeTypography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 物品网格
        items(items, key = { it.id }) { item ->
            val name = itemMap[item.id]?.name
            DepotItemCell(item, name)
        }
    }
}

@Composable
private fun DepotEmptyState(modifier: Modifier, statusMessage: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        MaaUiText(
            text = stringResource(R.string.maa_depot),
            style = ThemeTypography.titleLarge,
            color = ThemeColors.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(16.dp))
        HintRow(stringResource(R.string.panel_depot_hint_scan))
        Spacer(Modifier.height(12.dp))
        HintRow(stringResource(R.string.panel_depot_hint_results))
        if (statusMessage.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            MaaUiText(
                text = statusMessage,
                style = ThemeTypography.bodySmall,
                color = ThemeColors.primary
            )
        }
    }
}

@Composable
private fun HintRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp),
            tint = ThemeColors.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(12.dp))
        MaaUiText(
            text = text,
            style = ThemeTypography.bodyMedium,
            color = ThemeColors.onSurfaceVariant,
        )
    }
}

@Composable
private fun DepotItemCell(item: DepotItem, name: String?) {
    MaaUiSurface(
        shape = RoundedCornerShape(6.dp),
        color = ThemeColors.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.widthIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MaaUiText(
                text = name ?: item.id,
                style = ThemeTypography.bodySmall.copy(fontSize = 11.sp),
                color = ThemeColors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            MaaUiText(
                text = "x${item.count}",
                style = ThemeTypography.bodyMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold,
                color = ThemeColors.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
