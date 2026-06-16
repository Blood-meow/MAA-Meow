package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.achievement.AchievementCategory
import com.aliothmoon.maameow.data.achievement.AchievementState
import com.aliothmoon.maameow.data.achievement.AchievementTextFormatter
import com.aliothmoon.maameow.data.achievement.getAchievementPlaceholder
import com.aliothmoon.maameow.ui.theme.ThemeColors
import com.aliothmoon.maameow.ui.viewmodel.AchievementViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Surface as MiuixSurface
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AchievementViewMiuix(
    navController: NavController,
    viewModel: AchievementViewModel = koinViewModel(),
) {
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val unlockedCount by viewModel.unlockedCount.collectAsStateWithLifecycle()
    val languageTag = LocalConfiguration.current.locales[0]?.toLanguageTag().orEmpty()

    LaunchedEffect(viewModel) {
        viewModel.onScreenOpened()
    }

    MiuixScaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MiuixTopAppBar(
                title = stringResource(R.string.achievement_title),
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
                bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                    .asPaddingValues().calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MiuixTextField(
                    value = searchText,
                    onValueChange = viewModel::updateSearchText,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                MiuixText(
                    text = stringResource(R.string.achievement_unlocked_count, unlockedCount, totalCount),
                    fontWeight = FontWeight.Medium,
                    color = ThemeColors.onSurfaceVariant,
                )
            }
            items(achievements, key = { it.definition.id }) { achievement ->
                AchievementCardMiuix(achievement, languageTag)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AchievementCardMiuix(achievement: AchievementState, languageTag: String) {
    val accent = achievementColorMiuix(achievement)
    val context = LocalContext.current
    MiuixSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MiuixTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = accent,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(
                        text = if (achievement.unlocked) {
                            achievement.definition.title.resolve(languageTag).formatAchievementPlaceholders(context)
                        } else {
                            stringResource(R.string.achievement_locked_title)
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = ThemeColors.onSurface,
                    )
                    MiuixSurface(
                        shape = RoundedCornerShape(10.dp),
                        color = MiuixTheme.colorScheme.primaryContainer,
                    ) {
                        MiuixText(
                            text = "#${achievement.definition.releasePhase}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = ThemeColors.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                MiuixText(
                    text = if (achievement.unlocked) {
                        achievement.definition.description.resolve(languageTag).formatAchievementPlaceholders(context)
                    } else {
                        stringResource(R.string.achievement_locked_desc)
                    },
                    color = ThemeColors.onSurfaceVariant,
                )
                MiuixText(
                    text = if (!achievement.definition.hidden || achievement.unlocked) {
                        achievement.definition.condition.resolve(languageTag).formatAchievementPlaceholders(context)
                    } else {
                        stringResource(R.string.achievement_locked_condition)
                    },
                    color = ThemeColors.primary,
                )
                if (!achievement.unlocked && achievement.progressive) {
                    MiuixLinearProgressIndicator(
                        progress = (achievement.progress.toFloat() / achievement.definition.target).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MiuixText(
                        text = stringResource(
                            R.string.achievement_progress,
                            achievement.progress,
                            achievement.definition.target,
                        ),
                        color = ThemeColors.onSurfaceVariant,
                    )
                }
                if (achievement.unlocked && achievement.unlockedAtMillis != null) {
                    MiuixText(
                        text = stringResource(
                            R.string.achievement_unlocked_at,
                            DateFormat.getDateTimeInstance().format(Date(achievement.unlockedAtMillis)),
                        ),
                        color = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun achievementColorMiuix(achievement: AchievementState): Color = when {
    !achievement.unlocked -> ThemeColors.outline
    achievement.definition.rare -> ThemeColors.tertiary
    achievement.definition.hidden -> ThemeColors.secondary
    achievement.definition.category == AchievementCategory.BUG_RELATED -> ThemeColors.error
    achievement.definition.category == AchievementCategory.AUTO_BATTLE -> ThemeColors.primary
    else -> ThemeColors.primary
}

private fun String.formatAchievementPlaceholders(context: android.content.Context): String {
    return AchievementTextFormatter.formatPlaceholders(this) { key -> context.getAchievementPlaceholder(key) }
}
