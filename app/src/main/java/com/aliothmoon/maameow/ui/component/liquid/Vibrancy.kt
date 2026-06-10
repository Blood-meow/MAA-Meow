// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).
// Adapted from tiann/KernelSU — Apache 2.0.

package com.aliothmoon.maameow.ui.component.liquid

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls

/**
 * Vibrancy effect — boosts saturation and contrast to simulate iOS-style vibrancy.
 * Use inside a [BackdropEffectScope] (i.e. within drawBackdrop's effects block).
 */
fun BackdropEffectScope.vibrancy(
    brightness: Float = 0f,
    contrast: Float = 1f,
    saturation: Float = 1.5f,
) {
    colorControls(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
    )
}