package com.aliothmoon.maameow.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.ui.theme.ThemeColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AchievementDebugViewMiuix(
    navController: NavController,
    repository: AchievementRepository = koinInject(),
) {
    val achievements by repository.achievements.collectAsStateWithLifecycle()
    val languageTag = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf(achievements.firstOrNull()?.definition?.id.orEmpty()) }

    LaunchedEffect(achievements) {
        if (achievements.isNotEmpty() && achievements.none { it.definition.id == selectedId }) {
            selectedId = achievements.first().definition.id
        }
    }

    MiuixScaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.achievement_debug_title),
                navigationIcon = {
                    MiuixIconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                color = MiuixTheme.colorScheme.surface,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MiuixSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MiuixTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MiuixText(
                            text = stringResource(R.string.achievement_debug_desc),
                            color = ThemeColors.onSurfaceVariant,
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MiuixButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { expanded = true },
                            ) {
                                MiuixText(selectedId.ifBlank { stringResource(R.string.achievement_debug_select_label) })
                            }
                            if (expanded) {
                                MiuixSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MiuixTheme.colorScheme.surface,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        achievements.forEach { state ->
                                            MiuixDropdownItem(
                                                text = "${state.definition.id} - ${state.definition.title.resolve(languageTag)}",
                                                onClick = {
                                                    selectedId = state.definition.id
                                                    expanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        MiuixButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedId.isNotBlank(),
                            onClick = {
                                coroutineScope.launch {
                                    repository.unlock(selectedId)
                                    Toast.makeText(context, R.string.achievement_debug_unlock_done, Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            MiuixText(stringResource(R.string.achievement_debug_unlock))
                        }
                        MiuixButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = achievements.any { !it.unlocked },
                            onClick = {
                                coroutineScope.launch {
                                    repository.unlockAll()
                                    Toast.makeText(context, R.string.achievement_debug_unlock_all_done, Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            MiuixText(stringResource(R.string.achievement_debug_unlock_all))
                        }
                        MiuixButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                coroutineScope.launch {
                                    repository.clearAllRecords()
                                    Toast.makeText(context, R.string.achievement_debug_clear_done, Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            MiuixText(stringResource(R.string.achievement_debug_clear_all))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/**
 * Lightweight Miuix dropdown implemented as a Surface-stacked column. Miuix
 * does not provide a stable DropdownMenu API in this version, so we render an
 * overlay Surface when [expanded] is true.
 */


@Composable
private fun MiuixDropdownItem(
    text: String,
    onClick: () -> Unit,
) {
    MiuixSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MiuixTheme.colorScheme.surface,
    ) {
        MiuixText(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = ThemeColors.onSurface,
        )
    }
}
