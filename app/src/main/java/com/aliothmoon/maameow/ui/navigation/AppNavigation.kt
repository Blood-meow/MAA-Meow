package com.aliothmoon.maameow.ui.navigation

import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.announcement.AnnouncementConfig
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.service.ExternalNotificationService
import com.aliothmoon.maameow.overlay.OverlayController
import com.aliothmoon.maameow.ui.component.dialog.AnnouncementDialog
import com.aliothmoon.maameow.ui.component.ResourceLoadingOverlay
import com.aliothmoon.maameow.ui.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.schedule.model.CountdownState
import com.aliothmoon.maameow.schedule.ui.CountdownDialog
import com.aliothmoon.maameow.schedule.ui.ScheduleEditView
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogView
import com.aliothmoon.maameow.ui.screen.notification.NotificationSettingsView
import com.aliothmoon.maameow.ui.screen.settings.ErrorLogView
import com.aliothmoon.maameow.ui.screen.settings.LogHistoryView
import com.aliothmoon.maameow.ui.screen.settings.TaskOverrideEditorView
import com.aliothmoon.maameow.ui.screen.settings.ThemeSettingsView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Routes handled by MainScreen (HorizontalPager tabs) */
private val mainTabRoutes = setOf(
    Routes.HOME,
    Routes.BACKGROUND_TASK,
    Routes.SCHEDULE,
    Routes.SETTINGS
)

@Composable
fun AppNavigation(
    backgroundTaskViewModel: BackgroundTaskViewModel,
    appSettings: AppSettingsManager = koinInject(),
    notificationService: ExternalNotificationService = koinInject(),
    overlayController: OverlayController = koinInject(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current

    var isFullscreen by remember { mutableStateOf(false) }
    var forceShowAnnouncement by remember { mutableStateOf(false) }
    var announcementDismissedOnce by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val runMode by appSettings.runMode.collectAsStateWithLifecycle()
    val announcementReadVersion by appSettings.announcementReadVersion.collectAsStateWithLifecycle()
    val language by appSettings.language.collectAsStateWithLifecycle()
    val pendingScheduledExecution by backgroundTaskViewModel.coordinator.pendingExecution.collectAsStateWithLifecycle()
    val scheduledCountdownState by backgroundTaskViewModel.coordinator.countdownState.collectAsStateWithLifecycle()

    val switchBackgroundModeMessage = stringResource(R.string.navigation_toast_switch_background_mode)

    // Navigate to scheduled task if needed
    LaunchedEffect(pendingScheduledExecution?.requestId) {
        if (pendingScheduledExecution != null && currentNavRoute != Routes.BACKGROUND_TASK) {
            navController.navigate(Routes.BACKGROUND_TASK) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Collect feedback messages
    LaunchedEffect(backgroundTaskViewModel) {
        backgroundTaskViewModel.coordinator.feedbackMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(backgroundTaskViewModel) {
        backgroundTaskViewModel.coordinator.countdownState.collect { state ->
            overlayController.updateCountdownState(state)
        }
    }

    LaunchedEffect(backgroundTaskViewModel) {
        overlayController.onCountdownClick = {
            backgroundTaskViewModel.onScheduledStartNow()
        }
    }

    LaunchedEffect(notificationService) {
        notificationService.feedbackMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val forwardEnterTransition = maaForwardEnterTransition()
    val forwardExitTransition = maaForwardExitTransition()
    val popEnterTransition = maaPopEnterTransition()
    val popExitTransition = maaPopExitTransition()

    // Is current route a main tab?
    val isOnMainTab = currentNavRoute in mainTabRoutes || currentNavRoute == null

    Box(modifier = Modifier.fillMaxSize()) {
        // MainScreen renders the HorizontalPager with all 4 tabs.
        // Always keep in composition to preserve pager state, but hide when on sub-page.
        MainScreen(
            navController = navController,
            backgroundTaskViewModel = backgroundTaskViewModel,
            onFullscreenChanged = { isFullscreen = it },
            onViewAnnouncement = { forceShowAnnouncement = true },
            visible = isOnMainTab && !isFullscreen,
        )

        // NavHost for sub-pages only (theme settings, notifications, logs, etc.)
        // Tab switching is handled entirely by MainScreen's HorizontalPager.
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
        ) {
            // Main tab routes - MainScreen handles rendering
            composable(route = Routes.HOME) { /* MainScreen renders */ }
            composable(route = Routes.BACKGROUND_TASK) { /* MainScreen renders */ }
            composable(route = Routes.SCHEDULE) { /* MainScreen renders */ }
            composable(route = Routes.SETTINGS) { /* MainScreen renders */ }

            // ── Sub-pages with forward navigation transitions ──
            composable(
                route = Routes.NOTIFICATION,
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                PredictivePopBackHandler { navController.popBackStack() }
                NotificationSettingsView()
            }

            composable(
                route = Routes.THEME_SETTINGS,
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                PredictivePopBackHandler { navController.popBackStack() }
                ThemeSettingsView(navController = navController)
            }

            composable(
                route = Routes.LOG_HISTORY,
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                PredictivePopBackHandler { navController.popBackStack() }
                LogHistoryView(navController = navController)
            }

            composable(
                route = Routes.ERROR_LOG,
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                PredictivePopBackHandler { navController.popBackStack() }
                ErrorLogView(navController = navController)
            }

            composable(
                route = Routes.SCHEDULE_EDIT,
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) { backStackEntry ->
                PredictivePopBackHandler { navController.popBackStack() }
                val strategyId = backStackEntry.arguments?.getString("strategyId")
                    .let { if (it == "new") null else it }
                ScheduleEditView(navController = navController, strategyId = strategyId)
            }

            composable(
                route = Routes.SCHEDULE_TRIGGER_LOG,
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                PredictivePopBackHandler { navController.popBackStack() }
                ScheduleTriggerLogView(navController = navController)
            }

            composable(
                route = Routes.TASK_OVERRIDE_EDITOR,
                enterTransition = { forwardEnterTransition },
                exitTransition = { forwardExitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                PredictivePopBackHandler { navController.popBackStack() }
                TaskOverrideEditorView(navController = navController)
            }
        }

        ResourceLoadingOverlay()

        // Global countdown dialog
        val countdown = scheduledCountdownState
        val hideCountdownDialog = runMode == RunMode.FOREGROUND
        if (countdown is CountdownState.Counting && !hideCountdownDialog) {
            CountdownDialog(
                state = countdown,
                onCancel = { backgroundTaskViewModel.onScheduledCountdownCancel() },
                onStartNow = { backgroundTaskViewModel.onScheduledStartNow() },
            )
        }

        // Announcement dialog
        val needsToShow = announcementReadVersion != AnnouncementConfig.CURRENT_VERSION
        val showAnnouncement = forceShowAnnouncement || (needsToShow && !announcementDismissedOnce)
        val announcementMarkdown = remember(showAnnouncement, language) {
            if (showAnnouncement) {
                AnnouncementConfig.loadContent(context, language)
            } else {
                null
            }
        }
        if (announcementMarkdown != null) {
            AnnouncementDialog(
                imageAssetPath = remember(language) { AnnouncementConfig.imageAssetPath(language) },
                markdown = announcementMarkdown,
                onDismiss = { dontShowAgain ->
                    forceShowAnnouncement = false
                    if (dontShowAgain) {
                        coroutineScope.launch {
                            appSettings.setAnnouncementReadVersion(AnnouncementConfig.CURRENT_VERSION)
                        }
                    } else {
                        announcementDismissedOnce = true
                    }
                },
            )
        }
    }
}

private fun maaForwardEnterTransition(): EnterTransition {
    val animationSpec = tween<IntOffset>(durationMillis = 360, easing = FastOutSlowInEasing)
    val fadeSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
    return slideInHorizontally(
        initialOffsetX = { it / 5 },
        animationSpec = animationSpec
    ) + fadeIn(animationSpec = fadeSpec) + scaleIn(
        initialScale = 0.985f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
    )
}

private fun maaForwardExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -it / 12 },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = 180))
}

private fun maaPopEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -it / 8 },
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing))
}

private fun maaPopExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = 220)) + scaleOut(
        targetScale = 0.985f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
    )
}

@Composable
private fun PredictivePopBackHandler(onBack: () -> Unit) {
    PredictiveBackHandler { progress ->
        try {
            progress.collect()
            onBack()
        } catch (_: CancellationException) {
        }
    }
}