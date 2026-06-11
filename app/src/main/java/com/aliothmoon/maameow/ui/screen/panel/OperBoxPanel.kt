package com.aliothmoon.maameow.ui.screen.panel

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.toolbox.OperBoxOperator
import com.aliothmoon.maameow.ui.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.utils.i18n.asString
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

@Composable
fun OperBoxPanel(
    modifier: Modifier = Modifier,
    viewModel: ToolboxViewModel = koinInject()
) {
    val result by viewModel.collector.operBoxResult.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val resolvedStatusMessage = statusMessage.asString()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val copyToastMessage = stringResource(R.string.panel_operbox_copy_toast)

    // 0 = 已拥有, 1 = 未拥有
    var selectedTab by remember { mutableIntStateOf(0) }

    val data = result
    if (data == null) {
        OperBoxEmptyState(modifier, resolvedStatusMessage)
        return
    }

    val operators = if (selectedTab == 0) data.owned else data.notOwned

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 顶部：Tab 切换 + 导出
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val tabs = listOf(
                        stringResource(R.string.panel_operbox_tab_owned, data.owned.size),
                        stringResource(R.string.panel_operbox_tab_not_owned, data.notOwned.size)
                    )
                    tabs.forEachIndexed { index, label ->
                        MaaUiText(
                            text = label,
                            style = ThemeTypography.bodySmall,
                            color = if (selectedTab == index)
                                ThemeColors.primary
                            else
                                ThemeColors.onSurfaceVariant,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedTab = index }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                TextButton(onClick = {
                    scope.launch {
                        val text = viewModel.exportOperBox()
                        val entry = ClipData.newPlainText("label", text).toClipEntry()
                        clipboard.setClipEntry(entry)
                    }
                    Toast.makeText(
                        context,
                        copyToastMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    MaaUiText(stringResource(R.string.common_export), style = ThemeTypography.bodySmall)
                }
            }
        }

        // 干员列表
        items(operators, key = { it.id }) { oper ->
            OperatorRow(oper)
        }
    }
}

@Composable
private fun OperBoxEmptyState(modifier: Modifier, statusMessage: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        MaaUiText(
            text = stringResource(R.string.panel_operbox_title),
            style = ThemeTypography.titleLarge,
            color = ThemeColors.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(16.dp))
        OperBoxHintRow(stringResource(R.string.panel_operbox_hint_scan))
        Spacer(Modifier.height(12.dp))
        OperBoxHintRow(stringResource(R.string.panel_operbox_hint_results))
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
private fun OperBoxHintRow(text: String) {
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
private fun OperatorRow(oper: OperBoxOperator) {
    val rarityColor = when (oper.rarity) {
        6 -> Color(0xFFFF6B35)
        5 -> Color(0xFFFFD700)
        4 -> Color(0xFF9C7CFF)
        3 -> Color(0xFF4FC3F7)
        2 -> Color(0xFFA5D6A7)
        else -> ThemeColors.onSurfaceVariant
    }

    MaaUiSurface(
        shape = RoundedCornerShape(6.dp),
        color = ThemeColors.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaaUiText(
                    text = "${oper.rarity}★",
                    style = ThemeTypography.bodySmall.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.Bold,
                    color = rarityColor
                )
                MaaUiText(
                    text = oper.name,
                    style = ThemeTypography.bodyMedium.copy(fontSize = 13.sp),
                    color = ThemeColors.onSurface
                )
            }
            if (oper.own) {
                MaaUiText(
                    text = stringResource(
                        R.string.panel_operbox_owned_meta,
                        oper.elite,
                        oper.level,
                        oper.potential
                    ),
                    style = ThemeTypography.bodySmall.copy(fontSize = 11.sp),
                    color = ThemeColors.onSurfaceVariant
                )
            }
        }
    }
}
