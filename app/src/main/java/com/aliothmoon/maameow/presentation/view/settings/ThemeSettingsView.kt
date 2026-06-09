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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aliothmoon.maameow.presentation.components.ui.MaaUiScaffold
import com.aliothmoon.maameow.presentation.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens
import org.koin.androidx.compose.koinViewModel

@Composable
private fun ThemeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

fun ThemeSettingsView(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val uiStyle by viewModel.uiStyle.collectAsStateWithLifecycle()
    val blurEnabled by viewModel.uiBlurEnabled.collectAsStateWithLifecycle()
    val floatingBottomBar by viewModel.uiFloatingBottomBar.collectAsStateWithLifecycle()
    val liquidGlassEnabled by viewModel.uiLiquidGlassEnabled.collectAsStateWithLifecycle()
    val monetEnabled by viewModel.uiMonetEnabled.collectAsStateWithLifecycle()
    val pageScale by viewModel.uiPageScale.collectAsStateWithLifecycle()
    MaaUiScaffold(
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
                ThemeSectionHeader(stringResource(R.string.settings_theme_style_section))
                InfoCard(
                    title = "",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentColor = contentColor,
                    contentPadding = PaddingValues(
                        horizontal = MaaDesignTokens.Card.innerPadding,
                        vertical = MaaDesignTokens.Spacing.listItemVertical
                    )
                ) {
                    UiStyleItem(
                        contentColor = contentColor,
                        selectedStyle = uiStyle,
                        onStyleSelected = viewModel::setUiStyle
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(MaaDesignTokens.Spacing.sectionGap))
                ThemeSectionHeader(stringResource(R.string.settings_theme_mode_section))
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
                    SettingsDivider(contentColor)
                    ThemeSwitchItem(
                        title = stringResource(R.string.settings_theme_monet_title),
                        description = stringResource(R.string.settings_theme_monet_desc),
                        contentColor = contentColor,
                        checked = monetEnabled,
                        onCheckedChange = viewModel::setUiMonetEnabled
                    )
                    SettingsDivider(contentColor)
                    PageScaleItem(
                        contentColor = contentColor,
                        scale = pageScale,
                        onScaleChange = viewModel::setUiPageScale
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(MaaDesignTokens.Spacing.sectionGap))
                ThemeSectionHeader(stringResource(R.string.settings_theme_effect_section))
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
private fun UiStyleItem(
    contentColor: Color,
    selectedStyle: AppSettingsManager.UiStyle,
    onStyleSelected: (AppSettingsManager.UiStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaaDesignTokens.Spacing.listItemVertical),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_theme_ui_style_title),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
        Text(
            text = stringResource(R.string.settings_theme_ui_style_desc),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.7f)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            val styles = listOf(
                AppSettingsManager.UiStyle.MATERIAL to stringResource(R.string.settings_theme_ui_style_material),
                AppSettingsManager.UiStyle.MIUIX to stringResource(R.string.settings_theme_ui_style_miuix)
            )
            styles.forEach { (style, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = style == selectedStyle,
                            onClick = { onStyleSelected(style) },
                            role = Role.RadioButton
                        )
                ) {
                    RadioButton(
                        selected = style == selectedStyle,
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
private fun PageScaleItem(
    contentColor: Color,
    scale: Float,
    onScaleChange: (Float) -> Unit
) {
    var sliderValue by remember(scale) { mutableFloatStateOf(scale) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaaDesignTokens.Spacing.listItemVertical),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_theme_page_scale_title),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
            Text(
                text = "${(sliderValue * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        Text(
            text = stringResource(R.string.settings_theme_page_scale_desc),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.7f)
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onScaleChange(sliderValue) },
            valueRange = 0.8f..1.3f,
            steps = 4
        )
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
