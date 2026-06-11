package com.aliothmoon.maameow.ui.screen.panel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.aliothmoon.maameow.ui.component.material.MaaUiSurface
import com.aliothmoon.maameow.ui.component.material.MaaUiText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.ui.viewmodel.ToolboxTab
import com.aliothmoon.maameow.ui.viewmodel.ToolboxViewModel
import org.koin.compose.koinInject
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.theme.ThemeTypography

@Composable
fun ToolboxPanel(
    modifier: Modifier = Modifier,
    viewModel: ToolboxViewModel = koinInject()
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // 子 Tab 按钮行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolboxTab.entries.forEach { tab ->
                val selected = currentTab == tab
                MaaUiSurface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected)
                        ThemeColors.primaryContainer
                    else
                        ThemeColors.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected)
                            ThemeColors.primary
                        else
                            ThemeColors.outlineVariant
                    ),
                    modifier = Modifier.clickable { viewModel.onTabChange(tab) }
                ) {
                    MaaUiText(
                        text = stringResource(tab.labelRes),
                        style = ThemeTypography.bodySmall.copy(fontSize = 12.sp),
                        color = if (selected)
                            ThemeColors.onPrimaryContainer
                        else
                            ThemeColors.onSurface,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 内容区
        when (currentTab) {
            ToolboxTab.MINI_GAME -> MiniGamePanel(delegate = viewModel.miniGame, modifier = Modifier.fillMaxSize())
            ToolboxTab.RECRUIT_CALC -> RecruitCalcPanel(modifier = Modifier.fillMaxSize())
            ToolboxTab.DEPOT -> DepotRecognitionPanel(modifier = Modifier.fillMaxSize())
            ToolboxTab.OPER_BOX -> OperBoxPanel(modifier = Modifier.fillMaxSize())
        }
    }
}
