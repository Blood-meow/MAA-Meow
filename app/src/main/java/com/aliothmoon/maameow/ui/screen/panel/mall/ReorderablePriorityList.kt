package com.aliothmoon.maameow.ui.screen.panel.mall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ReorderablePriorityList(
    items: List<String>,
    enabled: Boolean,
    isReorderMode: Boolean,
    onItemsReordered: (List<String>) -> Unit,
    onItemRemoved: (Int) -> Unit,
    onDraggingChanged: (Boolean) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent
    ) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = ThemeColors.surface,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = ThemeColors.outlineVariant,
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.panel_mall_priority_empty),
                    style = ThemeTypography.bodySmall,
                    color = ThemeColors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            ReorderableColumn(
                list = items,
                onSettle = { from, to ->
                    val mutableList = items.toMutableList()
                    val item = mutableList.removeAt(from)
                    mutableList.add(to, item)
                    onItemsReordered(mutableList)
                },
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) { index, item, isDragging ->
                key(item) {
                    ReorderableItem {
                        PriorityItemRow(
                            item = item,
                            isDragging = isDragging,
                            isReorderMode = isReorderMode,
                            enabled = enabled,
                            onRemove = { onItemRemoved(index) },
                            modifier = if (isReorderMode) Modifier.longPressDraggableHandle(
                                onDragStarted = { onDraggingChanged(true) },
                                onDragStopped = { onDraggingChanged(false) }
                            ) else Modifier
                        )
                    }
                }
            }
        }
    }
}
