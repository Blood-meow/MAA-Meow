package com.aliothmoon.maameow.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Press feedback: scales element down slightly on press.
 * Use with [Modifier.pointerInput] or as a click handler wrapper.
 */
fun Modifier.pressableScale(pressed: Boolean): Modifier {
    return this.graphicsLayer {
        val scale = if (pressed) 0.95f else 1f
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Shared-axis style page transitions inspired by Material motion spec.
 * Forward navigation: slide from right with scale-up + fade.
 * Back navigation: slide to right with scale-down + fade.
 */
object MaaAnimations {

    // ---- Page transitions (inspired by rikkahub / miuix) ----
    private const val PAGE_DURATION = 300

    private val pageEasing = FastOutSlowInEasing

    /** Forward enter: new page slides in from right, full width */
    fun sharedAxisForwardEnter(): EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(PAGE_DURATION, easing = pageEasing)
        ) + fadeIn(
            animationSpec = tween(PAGE_DURATION, easing = LinearEasing)
        )

    /** Forward exit: old page slides left + shrinks to 0.7 + fades out */
    fun sharedAxisForwardExit(): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 2 },
            animationSpec = tween(PAGE_DURATION, easing = pageEasing)
        ) + scaleOut(
            targetScale = 0.7f,
            animationSpec = tween(PAGE_DURATION, easing = pageEasing)
        ) + fadeOut(
            animationSpec = tween(PAGE_DURATION, easing = LinearEasing)
        )

    /** Pop enter: underlying page slides in from left + grows from 0.7 + fades in */
    fun sharedAxisPopEnter(): EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 2 },
            animationSpec = tween(PAGE_DURATION, easing = pageEasing)
        ) + scaleIn(
            initialScale = 0.7f,
            animationSpec = tween(PAGE_DURATION, easing = pageEasing)
        ) + fadeIn(
            animationSpec = tween(PAGE_DURATION, easing = LinearEasing)
        )

    /** Pop exit: current page slides out to right, full width */
    fun sharedAxisPopExit(): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(PAGE_DURATION, easing = pageEasing)
        ) + fadeOut(
            animationSpec = tween(PAGE_DURATION, easing = LinearEasing)
        )

    // ---- Tab transitions (horizontal slide + crossfade for page-like feel) ----
    private const val TAB_DURATION = 250

    /** Tab enter: new tab slides in from right + fades in */
    fun tabEnter(): EnterTransition =
        fadeIn(animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)) +
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth / 4 },
            animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)
        )

    /** Tab exit: old tab slides out to left + fades out */
    fun tabExit(): ExitTransition =
        fadeOut(animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)
        )

    /** Tab pop enter: underlying tab slides in from left + fades in */
    fun tabPopEnter(): EnterTransition =
        fadeIn(animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)) +
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)
        )

    /** Tab pop exit: current tab slides out to right + fades out */
    fun tabPopExit(): ExitTransition =
        fadeOut(animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth / 4 },
            animationSpec = tween(TAB_DURATION, easing = FastOutSlowInEasing)
        )

    // ---- Staggered list item enter ----
    private const val STAGGER_DELAY = 60
    private const val STAGGER_DURATION = 350

    /**
     * Composable that makes its content appear with a staggered slide-up + fade-in.
     * Uses a one-shot [LaunchedEffect] that only fires once per composition lifecycle.
     *
     * @param index The item index in the list; used to compute delay.
     * @param staggerDelayMs Delay between each item (ms).
     */
    @Composable
    fun StaggeredItemVisibility(
        index: Int,
        staggerDelayMs: Int = STAGGER_DELAY,
        content: @Composable () -> Unit
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(index) {
            delay((index * staggerDelayMs).toLong())
            visible = true
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 5 },
                animationSpec = tween(STAGGER_DURATION, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(STAGGER_DURATION, easing = LinearEasing)
            )
        ) {
            content()
        }
    }

    // ---- Expand/Collapse transition ----

    /**
     * Expand vertically from top with fade.
     */
    fun expandEnter(): EnterTransition =
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(200))

    /**
     * Collapse vertically to top with fade.
     */
    fun collapseExit(): ExitTransition =
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(200))

    // ---- Press feedback ----

    /**
     * Animated scale for press feedback. Apply via [graphicsLayer].
     */
    @Composable
    fun animatePressScale(interactionSource: MutableInteractionSource): Float {
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.96f else 1f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 400f
            ),
            label = "pressScale"
        )
        return scale
    }

    // ---- Breathing glow ----

    /**
     * A breathing glow effect that animates opacity.
     */
    @Composable
    fun BreathingGlow(
        color: Color,
        modifier: Modifier = Modifier,
        maxAlpha: Float = 0.5f,
        durationMillis: Int = 2000
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = maxAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        Box(
            modifier = modifier.drawBehind {
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = size.maxDimension / 2f
                )
            }
        )
    }


}
