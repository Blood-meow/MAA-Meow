package com.aliothmoon.maameow.ui.screen.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import com.aliothmoon.maameow.ui.component.material.MaaUiCardContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.aliothmoon.maameow.ui.component.material.MaaUiCheckbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.TaskChainNode
import sh.calvin.reorderable.ReorderableColumn
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

/**
 * 左侧任务列表（支持模式切换、拖拽排序、勾选、新增任务入口）
 */
@Composable
fun TaskListPanel(
    nodes: List<TaskChainNode>,
    selectedNodeId: String?,
    isEditMode: Boolean,
    isAddingTask: Boolean,
    isProfileMode: Boolean,
    onNodeEnabledChange: (String, Boolean) -> Unit,
    onNodeSelected: (String) -> Unit,
    onNodeMove: (Int, Int) -> Unit,
    onToggleEditMode: () -> Unit,
    onToggleAddingTask: () -> Unit,
    onToggleProfileMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(IntrinsicSize.Max)) {
        // 配置选择按钮 - 在编辑任务按钮上方
        MaaUiCardContainer(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleProfileMode() },
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isProfileMode) ThemeColors.primary else ThemeColors.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isProfileMode) 2.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isProfileMode) Icons.Default.Check else Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isProfileMode) ThemeColors.onPrimary else ThemeColors.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isProfileMode) stringResource(R.string.common_done) else stringResource(R.string.panel_task_list_edit_config),
                    style = ThemeTypography.bodyMedium,
                    fontWeight = if (isProfileMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isProfileMode) ThemeColors.onPrimary else ThemeColors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 编辑任务按钮 - 具备高亮状态
        MaaUiCardContainer(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleEditMode() },
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEditMode) ThemeColors.primary else ThemeColors.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isEditMode) 2.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isEditMode) ThemeColors.onPrimary else ThemeColors.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditMode) stringResource(R.string.common_done) else stringResource(R.string.panel_task_list_edit_tasks),
                    style = ThemeTypography.bodyMedium,
                    fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEditMode) ThemeColors.onPrimary else ThemeColors.onSurface
                )
            }
        }

        // 新增任务按钮 - 仅在编辑模式下显示
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                MaaUiCardContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAddingTask() },
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAddingTask) ThemeColors.primaryContainer else ThemeColors.surface
                    ),
                    border = BorderStroke(1.dp, ThemeColors.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ThemeColors.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.panel_task_list_add),
                            style = ThemeTypography.bodyMedium,
                            color = ThemeColors.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ReorderableColumn(
            list = nodes,
            onSettle = { fromIndex, toIndex -> onNodeMove(fromIndex, toIndex) },
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { _, node, _ ->
            key(node.id) {
                ReorderableItem {
                    TaskNodeRow(
                        node = node,
                        isSelected = selectedNodeId == node.id,
                        isEditMode = isEditMode,
                        onEnabledChange = { enabled -> onNodeEnabledChange(node.id, enabled) },
                        onSelected = { onNodeSelected(node.id) },
                        modifier = Modifier.longPressDraggableHandle()
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskNodeRow(
    node: TaskChainNode,
    isSelected: Boolean,
    isEditMode: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaaUiCardContainer(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ThemeColors.primaryContainer else ThemeColors.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, ThemeColors.primary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected() }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 在编辑模式下也可以保留勾选框，或者隐藏以展示纯粹的排序视图
            // 这里根据用户反馈“保持清爽”，我们依然显示勾选框以便快速切换状态，但调整间距
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                MaaUiCheckbox(
                    checked = node.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = node.name,
                style = ThemeTypography.bodyMedium,
                color = if (isSelected) ThemeColors.onPrimaryContainer else ThemeColors.onSurface,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
