package com.aliothmoon.maameow.ui

import androidx.compose.runtime.compositionLocalOf
import com.aliothmoon.maameow.data.preferences.AppSettingsManager

val LocalUiStyle = compositionLocalOf { AppSettingsManager.UiStyle.MATERIAL }

val isMiuixUi: Boolean
    @androidx.compose.runtime.Composable
    get() = LocalUiStyle.current == AppSettingsManager.UiStyle.MIUIX
