package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.theme.ThemeColors
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

@Composable
fun MaaUiScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    if (isMiuixUi) {
        MiuixScaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content
        )
    } else {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content
        )
    }
}

/**
 * TopAppBar with optional back navigation, matching the project's
 * isMiuixUi toggle. Falls back to Material3 when Miuix is disabled.
 *
 * @param onBack when null, no back icon is shown
 * @param scrollBehavior Miuix scroll behavior; ignored on Material side
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaaUiTopAppBarWithBack(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null
) {
    if (isMiuixUi) {
        MiuixTopAppBar(
            title = title,
            navigationIcon = {
                if (onBack != null) {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accessibility_navigation)
                        )
                    }
                }
            },
            actions = actions,
            scrollBehavior = scrollBehavior
        )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accessibility_navigation)
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ThemeColors.background,
                titleContentColor = ThemeColors.onSurface,
                navigationIconContentColor = ThemeColors.primary,
                actionIconContentColor = ThemeColors.primary
            )
        )
    }
}
