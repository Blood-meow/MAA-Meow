package com.aliothmoon.maameow.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.aliothmoon.maameow.presentation.view.home.HomeView
import com.aliothmoon.maameow.presentation.view.background.BackgroundTaskView
import com.aliothmoon.maameow.schedule.ui.ScheduleListView
import com.aliothmoon.maameow.presentation.view.settings.SettingsView
import com.aliothmoon.maameow.theme.MaaAnimations
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import org.koin.compose.koinInject

const val MAIN_PAGE_COUNT = 4

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState?> { null }

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
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = tween(easing = MaaAnimations.springEasing, durationMillis = duration)
                )
            } finally {
                isNavigating = false
                if (pagerState.currentPage != targetIndex) {
                    selectedPage = pagerState.currentPage
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

@Composable
fun MainScreen(
    navController: androidx.navigation.NavController,
    backgroundTaskViewModel: com.aliothmoon.maameow.presentation.viewmodel.BackgroundTaskViewModel,
    onFullscreenChanged: (Boolean) -> Unit,
    onViewAnnouncement: () -> Unit = {},
    visible: Boolean = true,
    appSettings: AppSettingsManager = koinInject()
) {
    val pagerState = rememberPagerState(pageCount = { MAIN_PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) { mainPagerState.syncPage() }
    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) { mainPagerState.syncPage() }

    val currentNavRoute = BottomNavTab.all[mainPagerState.selectedPage].route
    val showBottomBar = visible

    val runMode by appSettings.runMode.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(
                    currentRoute = currentNavRoute,
                    onTabSelected = { tab ->
                        val index = BottomNavTab.all.indexOf(tab)
                        if (index >= 0) mainPagerState.animateToPage(index)
                    }
                )
            }
        }
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
