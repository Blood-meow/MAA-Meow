package com.aliothmoon.maameow.ui.navigation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.screen.background.BackgroundTaskView
import com.aliothmoon.maameow.ui.screen.home.HomeView
import com.aliothmoon.maameow.ui.screen.settings.SettingsView
import com.aliothmoon.maameow.ui.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.schedule.ui.ScheduleListView
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs

const val MAIN_PAGE_COUNT = 4

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState?> { null }

/**
 * Total vertical space the floating bottom bar occupies at the bottom of
 * the screen in Miuix mode. Sub-pages (BackgroundTaskView,
 * ScheduleListView, ...) add this to any bottom-anchored widget's padding
 * so it does not collide with the floating bar.
 *
 * Mirrors the offset applied by `MiuixBottomNavigation` for the floating
 * bar: 12.dp margin + WindowInsets.navigationBars + 64.dp bar height.
 * Defaults to 0.dp in Material mode (Scaffold handles its own bottom
 * inset) or when the floating bar is disabled.
 */
val LocalFloatingBottomBarHeight = staticCompositionLocalOf { 0.dp }

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        val distance = abs(targetIndex - pagerState.currentPage).coerceAtLeast(2)
        val duration = 100 * distance + 100
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages = targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = tween(easing = EaseInOut, durationMillis = duration)
                )
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: kotlinx.coroutines.CoroutineScope = rememberCoroutineScope()
): MainPagerState {
    return remember(pagerState, coroutineScope) {
        MainPagerState(pagerState, coroutineScope)
    }
}

/**
 * Main screen with HorizontalPager for smooth tab switching.
 * bottomBar is ALWAYS passed to Scaffold, content always uses scaffold
 * paddingValues. The floating bar handles its own positioning internally
 * (12dp + navigationBars).
 */
@Composable
fun MainScreen(
    navController: NavController,
    backgroundTaskViewModel: BackgroundTaskViewModel,
    onFullscreenChanged: (Boolean) -> Unit,
    onViewAnnouncement: () -> Unit = {},
    visible: Boolean = true,
    appSettings: AppSettingsManager = koinInject()
) {
    val pagerState = rememberPagerState(pageCount = { MAIN_PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)
    val miuix = isMiuixUi
    val uiBlurEnabled by appSettings.uiBlurEnabled.collectAsStateWithLifecycle()
    val uiFloatingBottomBar by appSettings.uiFloatingBottomBar.collectAsStateWithLifecycle()

    // Create backdrop for Miuix mode (used by FloatingBottomBar blur effects)
    val backdrop = if (miuix) {
        val surfaceColor = MiuixTheme.colorScheme.surface
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }
    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) { mainPagerState.syncPage() }
    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) { mainPagerState.syncPage() }

    // Bottom bar composable — always built, passed to scaffold in both modes
    val bottomBar: @Composable () -> Unit = if (visible) {
        @Composable {
            Box(modifier = Modifier.fillMaxWidth()) {
                AppBottomNavigation(
                    currentRoute = BottomNavTab.all[mainPagerState.selectedPage].route,
                    onTabSelected = { tab ->
                        val index = BottomNavTab.all.indexOf(tab)
                        if (index >= 0) mainPagerState.animateToPage(index)
                    },
                    backdrop = backdrop
                )
            }
        }
    } else {
        { {} }
    }

    // NOTE: previously had a shared `pagerContent: @Composable (Modifier) -> Unit`
    // lambda here, but it was defined OUTSIDE the `if (miuix)` block so when
    // called from within the Miuix branch's CompositionLocalProvider, the
    // child pages no longer inherited LocalFloatingBottomBarHeight (saw
    // 0.dp default instead of the floating bar's true height). The
    // HorizontalPager is now inlined directly inside each branch below,
    // preserving the provider's scope for all child pages.

    if (miuix) {
        val useFloatingBar = uiFloatingBottomBar && visible
        val floatingBarHeight = if (useFloatingBar) {
            12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 64.dp
        } else {
            0.dp
        }
        CompositionLocalProvider(
            LocalMainPagerState provides mainPagerState,
            LocalFloatingBottomBarHeight provides floatingBarHeight,
        ) {
            Box(Modifier.fillMaxSize()) {
                // Child pages have their own MiuixScaffold with TopAppBar,
                // which handles status bar insets. MainScreen only needs to pass
                // WindowInsets(0) to avoid double top padding.
                MiuixScaffold(
                    bottomBar = if (useFloatingBar) {{}} else bottomBar,
                    containerColor = if (useFloatingBar) Color.Transparent
                        else MiuixTheme.colorScheme.surface,
                    contentWindowInsets = WindowInsets(0)
                ) { paddingValues ->
                    Box(
                        modifier = if (uiBlurEnabled && backdrop != null)
                            Modifier.layerBackdrop(backdrop) else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            HorizontalPager(
                                state = mainPagerState.pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 3,
                                userScrollEnabled = true
                            ) { page ->
                                when (page) {
                                    0 -> HomeView(navController = navController)
                                    1 -> BackgroundTaskView(
                                        onFullscreenChanged = onFullscreenChanged,
                                        viewModel = backgroundTaskViewModel,
                                    )
                                    2 -> ScheduleListView(navController = navController)
                                    3 -> SettingsView(
                                        navController = navController,
                                        onViewAnnouncement = onViewAnnouncement,
                                    )
                                }
                            }
                        }
                    }
                }
                if (useFloatingBar) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)) {
                        bottomBar()
                    }
                }
            }
        }
    } else {
        // Material mode: standard scaffold
        CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
            Scaffold(
                bottomBar = if (visible) ({
                    AppBottomNavigation(
                        currentRoute = BottomNavTab.all[mainPagerState.selectedPage].route,
                        onTabSelected = { tab ->
                            val index = BottomNavTab.all.indexOf(tab)
                            if (index >= 0) mainPagerState.animateToPage(index)
                        },
                    )
                }) else {{}}
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                        .alpha(if (visible) 1f else 0.99f)
                ) {
                    HorizontalPager(
                        state = mainPagerState.pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 3,
                        userScrollEnabled = true
                    ) { page ->
                        when (page) {
                            0 -> HomeView(navController = navController)
                            1 -> BackgroundTaskView(
                                onFullscreenChanged = onFullscreenChanged,
                                viewModel = backgroundTaskViewModel,
                            )
                            2 -> ScheduleListView(navController = navController)
                            3 -> SettingsView(
                                navController = navController,
                                onViewAnnouncement = onViewAnnouncement,
                            )
                        }
                    }
                }
            }
        }
    }
}
