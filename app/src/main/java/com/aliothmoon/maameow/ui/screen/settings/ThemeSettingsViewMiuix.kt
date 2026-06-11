package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.WaterDrop
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Switch
import androidx.compose.material3.Icon
import top.yukonga.miuix.kmp.preference.SliderPreference
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.ui.theme.MaaDesignTokens
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.asString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ThemeSettingsViewMiuix(
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

    val scrollBehavior = MiuixScrollBehavior()

    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.settings_theme_settings_title),
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ThemePreviewMiuix(
                    themeMode = themeMode,
                    uiStyle = uiStyle,
                    blurEnabled = blurEnabled,
                    floatingBottomBar = floatingBottomBar,
                    liquidGlassEnabled = liquidGlassEnabled,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    resourceVersion = resourceVersion,
                    appVersion = appVersion,
                    serviceStatusText = uiState.serviceStatusText.asString(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                ThemeModeSegmentMiuix(
                    selectedMode = themeMode,
                    onModeSelected = viewModel::setThemeMode,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                SettingsCardMiuix(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    rows = listOf(
                        {
                            ThemeChoiceRowMiuix(
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
                                ThemeSwitchRowMiuix(
                                    icon = Icons.Rounded.Palette,
                                    title = stringResource(R.string.settings_theme_monet_title),
                                    summary = stringResource(R.string.settings_theme_monet_desc),
                                    checked = monetEnabled,
                                    onCheckedChange = viewModel::setUiMonetEnabled
                                )
                                AnimatedVisibility(visible = monetEnabled) {
                                    KeyColorPickerMiuix(
                                        selectedColor = uiKeyColor,
                                        onColorSelected = viewModel::setUiKeyColor
                                    )
                                }
                            }
                        }
                    )
                )
            }

            item {
                SettingsCardMiuix(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    rows = listOf(
                        {
                            ThemeSwitchRowMiuix(
                                icon = Icons.Rounded.BlurOn,
                                title = stringResource(R.string.settings_theme_blur_title),
                                summary = stringResource(R.string.settings_theme_blur_desc),
                                checked = blurEnabled,
                                onCheckedChange = viewModel::setUiBlurEnabled
                            )
                        },
                        {
                            ThemeSwitchRowMiuix(
                                icon = Icons.Rounded.ViewAgenda,
                                title = stringResource(R.string.settings_theme_floating_bar_title),
                                summary = stringResource(R.string.settings_theme_floating_bar_desc),
                                checked = floatingBottomBar,
                                onCheckedChange = viewModel::setUiFloatingBottomBar
                            )
                        },
                        {
                            ThemeSwitchRowMiuix(
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

            item {
                FontSizeSectionMiuix(
                    fontSizeScale = fontSizeScale,
                    onFontSizeChange = viewModel::setFontSizeScale
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewMiuix(
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
    val surfaceVariant = MiuixTheme.colorScheme.surfaceVariant
    val onSurface = MiuixTheme.colorScheme.onSurface
    val primary = MiuixTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        MiuixSurface(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .aspectRatio(0.58f)
                .border(1.5.dp, MiuixTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MiuixTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiuixText(
                        text = stringResource(R.string.home_app_title),
                        style = MiuixTheme.textStyles.headline2,
                        color = onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviewInfoMiuix(
                        label = stringResource(R.string.home_display_mode),
                        value = when (themeMode) {
                            AppSettingsManager.ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            AppSettingsManager.ThemeMode.WHITE -> stringResource(R.string.settings_theme_white)
                            AppSettingsManager.ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            AppSettingsManager.ThemeMode.PURE_DARK -> stringResource(R.string.settings_theme_pure_dark)
                        },
                        accent = primary
                    )
                    PreviewInfoMiuix(
                        label = stringResource(R.string.home_screen_resolution),
                        value = "${screenWidth}×${screenHeight}",
                        accent = primary
                    )
                    PreviewInfoMiuix(
                        label = stringResource(R.string.home_resource_version_label),
                        value = resourceVersion,
                        accent = primary
                    )
                    PreviewInfoMiuix(
                        label = stringResource(R.string.home_app_version_label),
                        value = appVersion,
                        accent = primary
                    )
                }

                MiuixSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                        .border(1.5.dp, MiuixTheme.colorScheme.outlineVariant, CircleShape),
                    shape = CircleShape,
                    color = surfaceVariant.copy(alpha = if (blurEnabled) 0.72f else 0.92f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PreviewDotMiuix(selected = true, icon = Icons.Rounded.Cottage)
                        PreviewDotMiuix(selected = false, icon = Icons.Rounded.PlayArrow)
                        PreviewDotMiuix(selected = false, icon = Icons.Rounded.DateRange)
                        PreviewDotMiuix(selected = false, icon = Icons.Rounded.Settings)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewInfoMiuix(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MiuixText(text = label, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accent))
            MiuixText(text = value, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        }
    }
}

@Composable
private fun PreviewDotMiuix(selected: Boolean, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (selected) 18.dp else 16.dp),
            tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.55f)
        )
        Box(
            modifier = Modifier
                .width(if (selected) 14.dp else 4.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.22f))
        )
    }
}

@Composable
private fun ThemeModeSegmentMiuix(
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
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        modes.forEachIndexed { index, (mode, icon) ->
            val selected = selectedMode == mode
            MiuixSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onModeSelected(mode) },
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    modes.lastIndex -> RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(0.dp)
                },
                color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary)
                }
            }
        }
    }
}

@Composable
private fun SettingsCardMiuix(
    modifier: Modifier = Modifier,
    rows: List<@Composable () -> Unit>
) {
    MiuixSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                row()
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = MaaDesignTokens.Separator.thickness,
                        color = MiuixTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChoiceRowMiuix(
    icon: ImageVector,
    title: String,
    summary: String,
    selectedIndex: Int,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsRowHeaderMiuix(icon = icon, title = title, summary = summary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                MiuixSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable { onSelected(index) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        MiuixText(
                            text = label,
                            style = MiuixTheme.textStyles.body2,
                            maxLines = 1,
                            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwitchRowMiuix(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsIconMiuix(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(text = title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
            MiuixText(text = summary, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRowHeaderMiuix(icon: ImageVector, title: String, summary: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsIconMiuix(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(text = title, style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
            MiuixText(text = summary, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        }
    }
}

@Composable
private fun SettingsIconMiuix(icon: ImageVector) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

private val presetColors = listOf(
    0L to Color(0xFF2B6BCA),
    0xFFF44336L to Color(0xFFF44336),
    0xFFE91E63L to Color(0xFFE91E63),
    0xFF9C27B0L to Color(0xFF9C27B0),
    0xFF673AB7L to Color(0xFF673AB7),
    0xFF3F51B5L to Color(0xFF3F51B5),
    0xFF2196F3L to Color(0xFF2196F3),
    0xFF00BCD4L to Color(0xFF00BCD4),
    0xFF009688L to Color(0xFF009688),
    0xFF4CAF50L to Color(0xFF4CAF50),
    0xFFFFEB3BL to Color(0xFFFFEB3B),
    0xFFFFC107L to Color(0xFFFFC107),
    0xFFFF9800L to Color(0xFFFF9800),
    0xFF795548L to Color(0xFF795548),
    0xFF607D8FL to Color(0xFF607D8F),
    0xFFFF9CA8L to Color(0xFFFF9CA8),
)

@Composable
private fun KeyColorPickerMiuix(selectedColor: Long, onColorSelected: (Long) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        HorizontalDivider(thickness = MaaDesignTokens.Separator.thickness, color = MiuixTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))
        MiuixText(text = stringResource(R.string.settings_key_color), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(presetColors.size) { index ->
                val (colorLong, colorObj) = presetColors[index]
                val selected = colorLong == selectedColor
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorObj)
                        .then(if (selected) Modifier.border(2.5.dp, MiuixTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable { onColorSelected(colorLong) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FontSizeSectionMiuix(fontSizeScale: Int, onFontSizeChange: (Int) -> Unit) {
    var localSlider by remember { mutableStateOf(fontSizeScale.toFloat()) }
    androidx.compose.runtime.LaunchedEffect(fontSizeScale) { localSlider = fontSizeScale.toFloat() }
    val current = localSlider.toInt().coerceIn(80, 110)
    val previewScale = current / 100f

    MiuixSurface(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column {
            // Page Scale icon header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsIconMiuix(Icons.Rounded.AspectRatio)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiuixText(text = stringResource(R.string.settings_font_size_title), style = MiuixTheme.textStyles.body1, color = MiuixTheme.colorScheme.onSurface)
                    MiuixText(text = stringResource(R.string.settings_font_size_summary), style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(MiuixTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                MiuixText(
                    text = stringResource(R.string.settings_font_size_preview),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1.copy(fontSize = (16f * previewScale).sp)
                )
            }
            SliderPreference(
                value = localSlider,
                onValueChange = { newValue: Float -> localSlider = newValue },
                onValueChangeFinished = { onFontSizeChange(localSlider.toInt().coerceIn(80, 110)) },
                title = "",
                summary = "",
                valueText = current.toString(),
                valueRange = 80f..110f,
                steps = 29,
                showKeyPoints = true,
                keyPoints = listOf(80f, 90f, 100f, 110f)
            )
        }
    }
}
