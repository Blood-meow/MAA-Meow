package com.aliothmoon.maameow.presentation.navigation

import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
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
import com.aliothmoon.maameow.presentation.components.AnnouncementDialog
import com.aliothmoon.maameow.presentation.components.ResourceLoadingOverlay
import com.aliothmoon.maameow.presentation.components.ui.MaaUiScaffold
import com.aliothmoon.maameow.presentation.components.ui.isMiuixUi
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import com.aliothmoon.maameow.presentation.view.background.BackgroundTaskView
import com.aliothmoon.maameow.presentation.view.home.HomeView
import com.aliothmoon.maameow.presentation.view.notification.NotificationSettingsView
import com.aliothmoon.maameow.presentation.view.settings.ErrorLogView
import com.aliothmoon.maameow.presentation.view.settings.LogHistoryView
import com.aliothmoon.maameow.presentation.view.settings.SettingsView
import com.aliothmoon.maameow.presentation.view.settings.TaskOverrideEditorView
import com.aliothmoon.maameow.presentation.view.settings.ThemeSettingsView
import com.aliothmoon.maameow.presentation.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.schedule.model.CountdownState
import com.aliothmoon.maameow.schedule.ui.CountdownDialog
import com.aliothmoon.maameow.schedule.ui.ScheduleEditView
import com.aliothmoon.maameow.schedule.ui.ScheduleListView
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

    // 执行模式状态 - 用于底部导航拦截
    val runMode by appSettings.runMode.collectAsStateWithLifecycle()
    val announcementReadVersion by appSettings.announcementReadVersion.collectAsStateWithLifecycle()
    val language by appSettings.language.collectAsStateWithLifecycle()
    val overlayControlMode by appSettings.overlayControlMode.collectAsStateWithLifecycle()
    val uiBlurEnabled by appSettings.uiBlurEnabled.collectAsStateWithLifecycle()
    val uiFloatingBottomBar by appSettings.uiFloatingBottomBar.collectAsStateWithLifecycle()
    val uiLiquidGlassEnabled by appSettings.uiLiquidGlassEnabled.collectAsStateWithLifecycle()
    val pendingScheduledExecution by backgroundTaskViewModel.coordinator.pendingExecution.collectAsStateWithLifecycle()
    val scheduledCountdownState by backgroundTaskViewModel.coordinator.countdownState.collectAsStateWithLifecycle()

    // 定义哪些页面属于主 Tab
    val mainTabs = listOf(Routes.HOME, Routes.BACKGROUND_TASK, Routes.SCHEDULE, Routes.SETTINGS)
    
    // 判断是否处于主 Tab 页面
    val isOnMainTab = currentNavRoute in mainTabs || currentNavRoute == null

    // 判断是否显示底部导航
    val showBottomBar = !isFullscreen && isOnMainTab
    val switchBackgroundModeMessage = stringResource(R.string.navigation_toast_switch_background_mode)

    LaunchedEffect(pendingScheduledExecution?.requestId) {
        if (pendingScheduledExecution != null && currentNavRoute != Routes.BACKGROUND_TASK) {
            navController.navigate(Routes.BACKGROUND_TASK) {
                popUpTo(Routes.HOME) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

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

    val tabRoutes = BottomNavTab.all.map { it.route }
    val blurBackdrop: LayerBackdrop? = rememberLayerBackdrop()
    val blurBackdrop: LayerBackdrop? = rememberLayerBackdrop()
    val forwardEnterTransition = maaForwardEnterTransition()
    val forwardExitTransition = maaForwardExitTransition()
    val popEnterTransition = maaPopEnterTransition()
    val popExitTransition = maaPopExitTransition()

    Box(modifier = Modifier.fillMaxSize()) {
        MaaUiScaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(
                            initialOffsetX = { it / 8 },
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 120))
                ) {
                    AppBottomNavigation(
                        currentRoute = currentNavRoute ?: Routes.HOME,
                        blurEnabled = isMiuix && uiBlurEnabled,
                        floating = isMiuix && uiFloatingBottomBar,
                        liquidGlass = isMiuix && uiLiquidGlassEnabled,
                        backdrop = blurBackdrop,
                        backdrop = blurBackdrop,
                        onTabSelected = { tab ->
                            if (tab.route == currentNavRoute) return@AppBottomNavigation

                            if (tab.route == Routes.BACKGROUND_TASK && runMode == RunMode.FOREGROUND) {
                                Toast.makeText(
                                    context,
                                    switchBackgroundModeMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@AppBottomNavigation
                            }

                            navController.navigate(tab.route) {
                                popUpTo(Routes.HOME) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                ) {
                    composable(
                        route = Routes.HOME,
                        enterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        exitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popEnterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popExitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) }
                    ) {
                        HomeView(navController = navController)
                    }

                    composable(
                        route = Routes.BACKGROUND_TASK,
                        enterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        exitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popEnterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popExitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) }
                    ) {
                        PredictivePopBackHandler { navController.popBackStack() }
                        BackgroundTaskView(
                            onFullscreenChanged = { isFullscreen = it },
                            viewModel = backgroundTaskViewModel,
                        )
                    }

                    composable(
                        route = Routes.SCHEDULE,
                        enterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        exitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popEnterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popExitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) }
                    ) {
                        PredictivePopBackHandler { navController.popBackStack() }
                        ScheduleListView(navController = navController)
                    }

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
                        route = Routes.SETTINGS,
                        enterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        exitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popEnterTransition = { maaTabEnterTransition(tabRoutes, initialState.destination.route, targetState.destination.route) },
                        popExitTransition = { maaTabExitTransition(tabRoutes, initialState.destination.route, targetState.destination.route) }
                    ) {
                        SettingsView(
                            navController = navController,
                            onViewAnnouncement = { forceShowAnnouncement = true },
                        )
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
            }
        }

        ResourceLoadingOverlay()

        // 全局定时任务倒计时弹窗（前台所有控制模式均不弹出对话框，静默处理）
        val countdown = scheduledCountdownState
        val hideCountdownDialog = runMode == RunMode.FOREGROUND
        if (countdown is CountdownState.Counting && !hideCountdownDialog) {
            CountdownDialog(
                state = countdown,
                onCancel = { backgroundTaskViewModel.onScheduledCountdownCancel() },
                onStartNow = { backgroundTaskViewModel.onScheduledStartNow() },
            )
        }

        // 长期公告弹窗：每次公告版本变更后首次启动自动弹出，或从设置中手动打开
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

private fun maaTabEnterTransition(tabRoutes: List<String>, initialRoute: String?, targetRoute: String?): EnterTransition {
    val initialIndex = tabRoutes.indexOf(initialRoute ?: Routes.HOME).coerceAtLeast(0)
    val targetIndex = tabRoutes.indexOf(targetRoute ?: Routes.HOME).coerceAtLeast(0)
    val direction = if (targetIndex >= initialIndex) 1 else -1
    return slideInHorizontally(
        initialOffsetX = { direction * it / 4 },
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)) + scaleIn(
        initialScale = 0.985f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
    )
}

private fun maaTabExitTransition(tabRoutes: List<String>, initialRoute: String?, targetRoute: String?): ExitTransition {
    val initialIndex = tabRoutes.indexOf(initialRoute ?: Routes.HOME).coerceAtLeast(0)
    val targetIndex = tabRoutes.indexOf(targetRoute ?: Routes.HOME).coerceAtLeast(0)
    val direction = if (targetIndex >= initialIndex) -1 else 1
    return slideOutHorizontally(
        targetOffsetX = { direction * it / 8 },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))
}

@Composable
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

@Composable
private fun maaForwardExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -it / 12 },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(durationMillis = 180))
}

@Composable
private fun maaPopEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -it / 8 },
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing))
}

@Composable
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
