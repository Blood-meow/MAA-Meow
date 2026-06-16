package com.aliothmoon.maameow.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * Hidden "Monet colors" viewer (easter egg, Miuix-only).
 *
 * Reached by tapping the version row 5 times within 2 seconds on the
 * Miuix settings page.
 */
@Composable
fun MonetColorsView(navController: NavController) {
    MonetColorsViewMiuix(navController = navController)
}
