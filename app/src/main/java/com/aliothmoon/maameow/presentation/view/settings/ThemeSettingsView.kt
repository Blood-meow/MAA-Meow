package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.presentation.components.InfoCard
import com.aliothmoon.maameow.presentation.components.TopAppBar
import com.aliothmoon.maameow.presentation.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeSettingsView(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val blurEnabled by viewModel.uiBlurEnabled.collectAsStateWithLifecycle()
    val floatingBottomBar by viewModel.uiFloatingBottomBar.collectAsStateWithLifecycle()
    val liquidGlassEnabled by viewModel.uiLiquidGlassEnabled.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_theme_settings_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() }
            )
        }
    ) { paddingValues ->
        val contentColor = MaterialTheme.colorScheme.onSurface
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SectionHeader(stringResource(R.string.settings_theme_mode_section))
                InfoCard(
                    title = "",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentColor = contentColor,
                    contentPadding = PaddingValues(
                        horizontal = MaaDesignTokens.Card.innerPadding,
                        vertical = MaaDesignTokens.Spacing.listItemVertical
                    )
                ) {
                    ThemeModeItem(
                        contentColor = contentColor,
                        selectedMode = themeMode,
                        onModeSelected = viewModel::setThemeMode
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(MaaDesignTokens.Spacing.sectionGap))
                SectionHeader(stringResource(R.string.settings_theme_effect_section))
                InfoCard(
                    title = "",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentColor = contentColor,
                    contentPadding = PaddingValues(
                        horizontal = MaaDesignTokens.Card.innerPadding,
                        vertical = MaaDesignTokens.Spacing.listItemVertical
                    )
                ) {
                    ThemeSwitchItem(
                        title = stringResource(R.string.settings_theme_blur_title),
                        description = stringResource(R.string.settings_theme_blur_desc),
                        contentColor = contentColor,
                        checked = blurEnabled,
                        onCheckedChange = viewModel::setUiBlurEnabled
                    )
                    SettingsDivider(contentColor)
                    ThemeSwitchItem(
                        title = stringResource(R.string.settings_theme_floating_bar_title),
                        description = stringResource(R.string.settings_theme_floating_bar_desc),
                        contentColor = contentColor,
                        checked = floatingBottomBar,
                        onCheckedChange = viewModel::setUiFloatingBottomBar
                    )
                    SettingsDivider(contentColor)
                    ThemeSwitchItem(
                        title = stringResource(R.string.settings_theme_liquid_glass_title),
                        description = stringResource(R.string.settings_theme_liquid_glass_desc),
                        contentColor = contentColor,
                        checked = liquidGlassEnabled,
                        onCheckedChange = viewModel::setUiLiquidGlassEnabled
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ThemeModeItem(
    contentColor: Color,
    selectedMode: AppSettingsManager.ThemeMode,
    onModeSelected: (AppSettingsManager.ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaaDesignTokens.Spacing.listItemVertical),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            val modes = listOf(
                AppSettingsManager.ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                AppSettingsManager.ThemeMode.WHITE to stringResource(R.string.settings_theme_white),
                AppSettingsManager.ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                AppSettingsManager.ThemeMode.PURE_DARK to stringResource(R.string.settings_theme_pure_dark)
            )
            modes.forEach { (mode, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = mode == selectedMode,
                            onClick = { onModeSelected(mode) },
                            role = Role.RadioButton
                        )
                ) {
                    RadioButton(
                        selected = mode == selectedMode,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSwitchItem(
    title: String,
    description: String,
    contentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaaDesignTokens.Spacing.listItemVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = contentColor)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDivider(contentColor: Color) {
    HorizontalDivider(
        thickness = MaaDesignTokens.Separator.thickness,
        color = contentColor.copy(alpha = 0.12f)
    )
}
