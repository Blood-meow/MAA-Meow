package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.ui.isMiuixUi
import com.aliothmoon.maameow.ui.theme.ThemeColors
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * Auto-switching Card container: MiuixCard in Miuix mode, Material3 Card otherwise.
 * This is a lower-level wrapper than MaaUiCard (which adds a title).
 * Accepts Material3 Card API but maps to MiuixCard for Miuix mode.
 */
@Composable
fun MaaUiCardContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    colors: androidx.compose.material3.CardColors? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (isMiuixUi) {
        // Pass resolved color directly to MiuixCard's containerColor parameter
        // so it handles press states correctly (not via external .background modifier).
        val resolvedContainerColor = containerColor.takeIf { it != Color.Unspecified }
            ?: colors?.containerColor
        val withBorder = if (border != null) modifier.border(border, shape) else modifier
        val withBackground = if (resolvedContainerColor != null) {
            withBorder.background(resolvedContainerColor, shape)
        } else {
            withBorder
        }
        MiuixCard(
            modifier = withBackground,
            pressFeedbackType = PressFeedbackType.Tilt,
            onClick = onClick,
        ) {
            content()
        }
    } else {
        val resolvedColors = colors ?: if (containerColor != Color.Unspecified)
            CardDefaults.cardColors(containerColor = containerColor)
        else CardDefaults.cardColors()
        val cardModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = resolvedColors,
            elevation = elevation,
            border = border
        ) {
            content()
        }
    }
}
