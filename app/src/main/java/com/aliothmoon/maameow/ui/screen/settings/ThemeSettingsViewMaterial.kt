package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import top.yukonga.miuix.kmp.preference.SliderPreference
import com.aliothmoon.maameow.ui.isMiuixUi
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.ui.component.material.TopAppBar
import com.aliothmoon.maameow.ui.component.material.MaaUiScaffold
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.ui.theme.MaaDesignTokens
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.asString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun ThemeSettingsViewMaterial(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    updateViewModel: UpdateViewModel = koinViewModel(),
    homeViewModel: HomeViewModel = koinViewModel(),
    permissionManager: PermissionManager = koinInject()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val uiStyle by viewModel.uiStyle.collectAsStateWithLifecycle()
    val blurEnabled by viewModel.uiBlurEnabled.collectAsStateWithLifecycle()
    val floatingBottomBar by viewModel.uiFloatingBottomBar.collectAsStateWithLifecycle()
    val liquidGlassEnabled by viewModel.uiLiquidGlassEnabled.collectAsStateWithLifecycle()
    val monetEnabled by viewModel.uiMonetEnabled.collectAsStateWithLifecycle()
    val uiKeyColor by viewModel.uiKeyColor.collectAsStateWithLifecycle()
    val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val (screenWidth, screenHeight) = Misc.getScreenSize(context)
    val resourceVersion by updateViewModel.currentResourceVersion.collectAsStateWithLifecycle()
    val appVersion = updateViewModel.currentAppVersion
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    MaaUiScaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_theme_settings_title),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MaaMeowThemePreview(
                    themeMode = themeMode,
                    uiStyle = uiStyle,
                    blurEnabled = uiStyle == AppSettingsManager.UiStyle.MIUIX && blurEnabled,
                    floatingBottomBar = uiStyle == AppSettingsManager.UiStyle.MIUIX && floatingBottomBar,
                    liquidGlassEnabled = uiStyle == AppSettingsManager.UiStyle.MIUIX && liquidGlassEnabled,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    resourceVersion = resourceVersion,
                    appVersion = appVersion,
                    serviceStatusText = uiState.serviceStatusText.asString(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                ThemeModeSegment(
                    selectedMode = themeMode,
                    onModeSelected = viewModel::setThemeMode,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                SettingsSectionCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    rows = listOf(
                        {
                            ThemeChoiceRow(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_theme_ui_style_title),
                                summary = stringResource(R.string.settings_theme_ui_style_desc),
                                selectedIndex = if (uiStyle == AppSettingsManager.UiStyle.MIUIX) 1 else 0,
                                options = listOf(
                                    stringResource(R.string.settings_theme_ui_style_material),
                                    stringResource(R.string.settings_theme_ui_style_miuix)
                                ),
                                onSelected = { index ->
                                    viewModel.setUiStyle(
                                        if (index == 1) AppSettingsManager.UiStyle.MIUIX
                                        else AppSettingsManager.UiStyle.MATERIAL
                                    )
                                }
                            )
                        },
                        {
                            Column {
                                ThemeSwitchRow(
                                    icon = Icons.Rounded.Palette,
                                    title = stringResource(R.string.settings_theme_monet_title),
                                    summary = stringResource(R.string.settings_theme_monet_desc),
                                    checked = monetEnabled,
                                    onCheckedChange = viewModel::setUiMonetEnabled
                                )
                                AnimatedVisibility(visible = monetEnabled) {
                                    KeyColorPicker(
                                        selectedColor = uiKeyColor,
                                        onColorSelected = viewModel::setUiKeyColor
                                    )
                                }
                            }
                        }
                    )
                )
            }

            if (uiStyle == AppSettingsManager.UiStyle.MIUIX) {
                item {
                    SettingsSectionCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        rows = listOf(
                            {
                                ThemeSwitchRow(
                                    icon = Icons.Rounded.BlurOn,
                                    title = stringResource(R.string.settings_theme_blur_title),
                                    summary = stringResource(R.string.settings_theme_blur_desc),
                                    checked = blurEnabled,
                                    onCheckedChange = viewModel::setUiBlurEnabled
                                )
                            },
                            {
                                ThemeSwitchRow(
                                    icon = Icons.Rounded.ViewAgenda,
                                    title = stringResource(R.string.settings_theme_floating_bar_title),
                                    summary = stringResource(R.string.settings_theme_floating_bar_desc),
                                    checked = floatingBottomBar,
                                    onCheckedChange = viewModel::setUiFloatingBottomBar
                                )
                            },
                            {
                                ThemeSwitchRow(
                                    icon = Icons.Rounded.WaterDrop,
                                    title = stringResource(R.string.settings_theme_liquid_glass_title),
                                    summary = stringResource(R.string.settings_theme_liquid_glass_desc),
                                    checked = liquidGlassEnabled,
                                    onCheckedChange = viewModel::setUiLiquidGlassEnabled
                                )
                            }
                        )
                    )
                }
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}

@Composable
private fun MaaMeowThemePreview(
    themeMode: AppSettingsManager.ThemeMode,
    uiStyle: AppSettingsManager.UiStyle,
    blurEnabled: Boolean,
    floatingBottomBar: Boolean,
    liquidGlassEnabled: Boolean,
    screenWidth: Int,
    screenHeight: Int,
    resourceVersion: String,
    appVersion: String,
    serviceStatusText: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val modeLabel = when (themeMode) {
        AppSettingsManager.ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
        AppSettingsManager.ThemeMode.WHITE -> stringResource(R.string.settings_theme_white)
        AppSettingsManager.ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
        AppSettingsManager.ThemeMode.PURE_DARK -> stringResource(R.string.settings_theme_pure_dark)
    }
    val uiStyleLabel = when (uiStyle) {
        AppSettingsManager.UiStyle.MATERIAL -> stringResource(R.string.settings_theme_ui_style_material)
        AppSettingsManager.UiStyle.MIUIX -> stringResource(R.string.settings_theme_ui_style_miuix)
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .aspectRatio(0.58f),
            shape = RoundedCornerShape(24.dp),
            color = colors.background,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_app_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviewScreenInfoCard(
                        modeLabel = serviceStatusText,
                        uiStyleLabel = uiStyleLabel,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        resourceVersion = resourceVersion,
                        appVersion = appVersion
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (floatingBottomBar || uiStyle == AppSettingsManager.UiStyle.MIUIX) 18.dp else 0.dp,
                            vertical = if (floatingBottomBar || uiStyle == AppSettingsManager.UiStyle.MIUIX) 10.dp else 0.dp
                        ),
                    shape = if (floatingBottomBar || uiStyle == AppSettingsManager.UiStyle.MIUIX) CircleShape else RoundedCornerShape(0.dp),
                    color = if (liquidGlassEnabled || uiStyle == AppSettingsManager.UiStyle.MIUIX) {
                        colors.surfaceContainer.copy(alpha = if (blurEnabled) 0.72f else 0.92f)
                    } else {
                        colors.surface
                    },
                    border = if (liquidGlassEnabled || uiStyle == AppSettingsManager.UiStyle.MIUIX) {
                        BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.12f))
                    } else {
                        null
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val isMiuix = uiStyle == AppSettingsManager.UiStyle.MIUIX
                        PreviewBottomDot(selected = true, icon = if (isMiuix) Icons.Rounded.Cottage else Icons.Filled.Home)
                        PreviewBottomDot(selected = false, icon = if (isMiuix) Icons.Rounded.PlayArrow else Icons.Filled.PlayArrow)
                        PreviewBottomDot(selected = false, icon = if (isMiuix) Icons.Rounded.DateRange else Icons.Filled.DateRange)
                        PreviewBottomDot(selected = false, icon = if (isMiuix) Icons.Rounded.Settings else Icons.Filled.Settings)
                    }
                }
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}

@Composable
private fun PreviewScreenInfoCard(
    modeLabel: String,
    uiStyleLabel: String,
    screenWidth: Int,
    screenHeight: Int,
    resourceVersion: String,
    appVersion: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PreviewInfoRow(
                label = stringResource(R.string.home_screen_resolution),
                value = "$screenWidth × $screenHeight",
                accent = MaterialTheme.colorScheme.primary
            )
            PreviewInfoRow(
                label = stringResource(R.string.home_resource_version_label),
                value = resourceVersion.ifBlank { stringResource(R.string.home_resource_not_installed) },
                accent = MaterialTheme.colorScheme.tertiary
            )
            PreviewInfoRow(
                label = stringResource(R.string.home_app_version_label),
                value = appVersion,
                accent = MaterialTheme.colorScheme.secondary
            )
            PreviewInfoRow(
                label = stringResource(R.string.home_display_mode),
                value = modeLabel,
                accent = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PreviewInfoRow(
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PreviewBottomDot(selected: Boolean, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (selected) 18.dp else 16.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
        Box(
            modifier = Modifier
                .width(if (selected) 14.dp else 4.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                )
        )
    }
}

@Composable
private fun ThemeModeSegment(
    selectedMode: AppSettingsManager.ThemeMode,
    onModeSelected: (AppSettingsManager.ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        AppSettingsManager.ThemeMode.SYSTEM to Icons.Filled.Brightness4,
        AppSettingsManager.ThemeMode.WHITE to Icons.Filled.Brightness7,
        AppSettingsManager.ThemeMode.DARK to Icons.Filled.Brightness3,
        AppSettingsManager.ThemeMode.PURE_DARK to Icons.Filled.Brightness1
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        modes.forEachIndexed { index, (mode, icon) ->
            val selected = selectedMode == mode
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onModeSelected(mode) },
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    modes.lastIndex -> RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null)
                }
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    rows: List<@Composable () -> Unit>
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                row()
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = MaaDesignTokens.Separator.thickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}

@Composable
private fun ThemeChoiceRow(
    icon: ImageVector,
    title: String,
    summary: String,
    selectedIndex: Int,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsRowHeader(icon = icon, title = title, summary = summary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable { onSelected(index) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}

@Composable
private fun ThemeSwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsIcon(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRowHeader(
    icon: ImageVector,
    title: String,
    summary: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsIcon(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(22.dp)
        )
    }
}

private val presetColors = listOf(
    0L to Color(0xFF2B6BCA),       // Default (arknights blue)
    0xFFF44336L to Color(0xFFF44336), // Red
    0xFFE91E63L to Color(0xFFE91E63), // Pink
    0xFF9C27B0L to Color(0xFF9C27B0), // Purple
    0xFF673AB7L to Color(0xFF673AB7), // Deep Purple
    0xFF3F51B5L to Color(0xFF3F51B5), // Indigo
    0xFF2196F3L to Color(0xFF2196F3), // Blue
    0xFF00BCD4L to Color(0xFF00BCD4), // Cyan
    0xFF009688L to Color(0xFF009688), // Teal
    0xFF4CAF50L to Color(0xFF4CAF50), // Green
    0xFFFFEB3BL to Color(0xFFFFEB3B), // Yellow
    0xFFFFC107L to Color(0xFFFFC107), // Amber
    0xFFFF9800L to Color(0xFFFF9800), // Orange
    0xFF795548L to Color(0xFF795548), // Brown
    0xFF607D8FL to Color(0xFF607D8F), // Blue Grey
    0xFFFF9CA8L to Color(0xFFFF9CA8), // Sakura
)

@Composable
private fun KeyColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        HorizontalDivider(
            thickness = MaaDesignTokens.Separator.thickness,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_key_color),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presetColors.size) { index ->
                val (colorLong, colorObj) = presetColors[index]
                val selected = colorLong == selectedColor
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorObj)
                        .then(
                            if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(colorLong) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}

@Composable
private fun FontSizeSlider(
    fontSizeScale: Int,
    onFontSizeChange: (Int) -> Unit
) {
    var localSlider by remember { mutableStateOf(fontSizeScale.toFloat()) }
    LaunchedEffect(fontSizeScale) { localSlider = fontSizeScale.toFloat() }
    val current = localSlider.toInt().coerceIn(80, 110)
    val previewScale = current / 100f
    val miuix = isMiuixUi

    if (miuix) {
        // Miuix mode: native SliderPreference
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_font_size_preview),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = (16f * previewScale).sp
                )
            }
            SliderPreference(
                value = localSlider,
                onValueChange = { newValue: Float -> localSlider = newValue },
                onValueChangeFinished = { onFontSizeChange(localSlider.toInt().coerceIn(80, 110)) },
                title = stringResource(R.string.settings_font_size_title),
                summary = stringResource(R.string.settings_font_size_summary),
                valueText = current.toString(),
                valueRange = 80f..110f,
                steps = 29,
                showKeyPoints = true,
                keyPoints = listOf(80f, 90f, 100f, 110f)
            )
        }
        return
    }

    // Material mode

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Preview bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_font_size_preview),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (16f * previewScale).sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_font_size_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_font_size_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = current.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Slider(
            value = localSlider,
            onValueChange = { localSlider = it },
            onValueChangeFinished = {
                onFontSizeChange(localSlider.toInt().coerceIn(80, 110))
            },
            valueRange = 80f..110f,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(80f, 90f, 100f, 110f).forEach { kp ->
                Text(
                    text = kp.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}

private val presetColors = listOf(
    0L to Color(0xFF2B6BCA),       // Default (arknights blue)
    0xFFF44336L to Color(0xFFF44336), // Red
    0xFFE91E63L to Color(0xFFE91E63), // Pink
    0xFF9C27B0L to Color(0xFF9C27B0), // Purple
    0xFF673AB7L to Color(0xFF673AB7), // Deep Purple
    0xFF3F51B5L to Color(0xFF3F51B5), // Indigo
    0xFF2196F3L to Color(0xFF2196F3), // Blue
    0xFF00BCD4L to Color(0xFF00BCD4), // Cyan
    0xFF009688L to Color(0xFF009688), // Teal
    0xFF4CAF50L to Color(0xFF4CAF50), // Green
    0xFFFFEB3BL to Color(0xFFFFEB3B), // Yellow
    0xFFFFC107L to Color(0xFFFFC107), // Amber
    0xFFFF9800L to Color(0xFFFF9800), // Orange
    0xFF795548L to Color(0xFF795548), // Brown
    0xFF607D8FL to Color(0xFF607D8F), // Blue Grey
    0xFFFF9CA8L to Color(0xFFFF9CA8), // Sakura
)

@Composable
private fun KeyColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        HorizontalDivider(
            thickness = MaaDesignTokens.Separator.thickness,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_key_color),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presetColors.size) { index ->
                val (colorLong, colorObj) = presetColors[index]
                val selected = colorLong == selectedColor
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorObj)
                        .then(
                            if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(colorLong) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Font Size setting at the bottom
        item {
            SettingsSectionCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                rows = listOf({
                    FontSizeSlider(
                        fontSizeScale = fontSizeScale,
                        onFontSizeChange = viewModel::setFontSizeScale
                    )
                })
            )
        }
    }
}
