package com.aliothmoon.maameow.ui.screen.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import com.aliothmoon.maameow.ui.component.material.MaaUiCheckbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.aliothmoon.maameow.ui.component.material.MaaUiHorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.component.material.MaaUiSwitch
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.toolbox.RecruitCalcResult
import com.aliothmoon.maameow.ui.component.INumericField
import com.aliothmoon.maameow.ui.component.RecruitTimeSelector
import com.aliothmoon.maameow.ui.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.utils.i18n.asString
import org.koin.compose.koinInject
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecruitCalcPanel(
    modifier: Modifier = Modifier,
    viewModel: ToolboxViewModel = koinInject()
) {
    val tags by viewModel.collector.recruitTags.collectAsStateWithLifecycle()
    val results by viewModel.collector.recruitResults.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val resolvedStatusMessage = statusMessage.asString()
    val config by viewModel.recruitConfig.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 4.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 总开关：自动设置招募时间
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MaaUiText(
                    text = stringResource(R.string.panel_recruit_calc_auto_time),
                    style = ThemeTypography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                MaaUiSwitch(
                    checked = config.autoSetTime,
                    onCheckedChange = {
                        viewModel.onRecruitConfigChange(config.copy(autoSetTime = it))
                    },
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        // 星级配置行
        val starLevels = listOf(3, 4, 5, 6)

        items(starLevels) { level ->
            val checked = when (level) {
                3 -> config.chooseLevel3
                4 -> config.chooseLevel4
                5 -> config.chooseLevel5
                else -> config.chooseLevel6
            }
            val time = when (level) {
                3 -> config.level3Time
                4 -> config.level4Time
                5 -> config.level5Time
                else -> 540
            }
            val timeEnabled = level != 6
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MaaUiCheckbox(
                    checked = checked,
                    onCheckedChange = {
                        val newConfig = when (level) {
                            3 -> config.copy(chooseLevel3 = it)
                            4 -> config.copy(chooseLevel4 = it)
                            5 -> config.copy(chooseLevel5 = it)
                            else -> config.copy(chooseLevel6 = it)
                        }
                        viewModel.onRecruitConfigChange(newConfig)
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
                MaaUiText(
                    text = stringResource(R.string.panel_recruit_calc_auto_select_tags, level),
                    style = ThemeTypography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                if (config.autoSetTime) {
                    RecruitTimeSelector(
                        totalMinutes = time,
                        enabled = timeEnabled,
                        onTimeChange = {
                            val newConfig = when (level) {
                                3 -> config.copy(level3Time = it)
                                4 -> config.copy(level4Time = it)
                                5 -> config.copy(level5Time = it)
                                else -> config
                            }
                            if (timeEnabled) {
                                viewModel.onRecruitConfigChange(newConfig)
                            }
                        }
                    )
                }
            }
        }

        // 检测到的标签
        if (tags.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MaaUiText(
                        text = stringResource(R.string.panel_recruit_calc_detected_tags),
                        style = ThemeTypography.labelSmall,
                        color = ThemeColors.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            MaaUiSurface(
                                shape = RoundedCornerShape(6.dp),
                                color = ThemeColors.secondaryContainer,
                            ) {
                                MaaUiText(
                                    text = tag,
                                    style = ThemeTypography.bodySmall,
                                    color = ThemeColors.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 分隔线（有标签或有结果时显示）
        if (tags.isNotEmpty() || results.isNotEmpty()) {
            item {
                MaaUiHorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = ThemeColors.outlineVariant
                )
            }
        }

        // 计算结果
        if (results.isNotEmpty()) {
            val sorted = results.sortedByDescending { it.level }
            items(sorted, key = { it.tags.joinToString() }) { result ->
                RecruitResultItem(result)
            }
        }

        // 空提示
        if (tags.isEmpty() && results.isEmpty()) {
            item {
                MaaUiText(
                    text = resolvedStatusMessage.ifBlank {
                        stringResource(R.string.panel_recruit_calc_empty_hint)
                    },
                    style = ThemeTypography.bodySmall,
                    color = ThemeColors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecruitResultItem(result: RecruitCalcResult) {
    val levelColor = when {
        result.level >= 6 -> Color(0xFFFF6B35)
        result.level >= 5 -> Color(0xFFFFD700)
        result.level >= 4 -> Color(0xFF9C7CFF)
        else -> ThemeColors.onSurfaceVariant
    }
    val bgColor = when {
        result.level >= 6 -> Color(0xFFFF6B35).copy(alpha = 0.08f)
        result.level >= 5 -> Color(0xFFFFD700).copy(alpha = 0.08f)
        else -> ThemeColors.surfaceVariant.copy(alpha = 0.4f)
    }

    MaaUiSurface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // 左侧星级徽标
            MaaUiSurface(
                shape = RoundedCornerShape(6.dp),
                color = levelColor.copy(alpha = 0.15f),
            ) {
                MaaUiText(
                    text = "${result.level}★",
                    style = ThemeTypography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // 右侧内容
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 标签组合
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    result.tags.forEach { tag ->
                        MaaUiSurface(
                            shape = RoundedCornerShape(4.dp),
                            color = ThemeColors.primaryContainer.copy(alpha = 0.6f),
                        ) {
                            MaaUiText(
                                text = tag,
                                style = ThemeTypography.labelSmall.copy(fontSize = 11.sp),
                                color = ThemeColors.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                // 干员列表
                if (result.operators.isNotEmpty()) {
                    MaaUiText(
                        text = result.operators.joinToString("  ") { it.name },
                        style = ThemeTypography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = ThemeColors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
