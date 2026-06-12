package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import com.aliothmoon.maameow.ui.isMiuixUi
import androidx.compose.foundation.shape.RoundedCornerShape

// ── RadioButton ──────────────────────────────────────────────

@Composable
fun MaaUiRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixUi) {
        MaaUiCheckbox(
            checked = selected,
            onCheckedChange = onClick?.let { cb -> { cb() } },
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            androidx.compose.material3.RadioButton(
                selected = selected, onClick = onClick, modifier = modifier, enabled = enabled,
            )
        }
    }
}

// ── Checkbox ─────────────────────────────────────────────────

@Composable
fun MaaUiCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixUi) {
        // Clip to caller-specified size so Miuix Checkbox doesn't overflow
        Box(
            modifier = modifier.clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            top.yukonga.miuix.kmp.basic.Checkbox(
                state = if (checked) ToggleableState.On else ToggleableState.Off,
                onClick = onCheckedChange?.let { cb -> { cb(!checked) } },
                modifier = Modifier.fillMaxSize(),
                enabled = enabled,
            )
        }
    } else {
        // Material mode: locally remove 48dp minimum so callers can size freely
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            androidx.compose.material3.Checkbox(
                checked = checked, onCheckedChange = onCheckedChange,
                modifier = modifier, enabled = enabled,
            )
        }
    }
}

// ── Switch ───────────────────────────────────────────────────

@Composable
fun MaaUiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            modifier = modifier, enabled = enabled,
        )
    } else {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            androidx.compose.material3.Switch(
                checked = checked, onCheckedChange = onCheckedChange,
                modifier = modifier, enabled = enabled,
            )
        }
    }
}

// ── Slider ───────────────────────────────────────────────────

@Composable
fun MaaUiSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Slider(
            value = value, onValueChange = onValueChange,
            modifier = modifier, enabled = enabled,
            valueRange = valueRange, steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    } else {
        androidx.compose.material3.Slider(
            value = value, onValueChange = onValueChange,
            modifier = modifier, enabled = enabled,
            valueRange = valueRange, steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

// ── Button ───────────────────────────────────────────────────

@Composable
fun MaaUiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick, modifier = modifier, enabled = enabled,
            insideMargin = contentPadding, content = content,
        )
    } else {
        androidx.compose.material3.Button(
            onClick = onClick, modifier = modifier, enabled = enabled,
            shape = shape, contentPadding = contentPadding,
            colors = colors ?: ButtonDefaults.buttonColors(),
            content = content,
        )
    }
}

// ── OutlinedButton ───────────────────────────────────────────

@Composable
fun MaaUiOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    colors: ButtonColors? = null,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick, modifier = modifier, enabled = enabled,
            insideMargin = contentPadding, content = content,
        )
    } else {
        androidx.compose.material3.OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled,
            shape = shape, contentPadding = contentPadding,
            colors = colors ?: ButtonDefaults.outlinedButtonColors(),
            border = border,
            content = content,
        )
    }
}

// ── TextButton ───────────────────────────────────────────────

@Composable
fun MaaUiTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick, modifier = modifier, enabled = enabled,
            insideMargin = contentPadding, content = content,
        )
    } else {
        androidx.compose.material3.TextButton(
            onClick = onClick, modifier = modifier, enabled = enabled,
            shape = shape, contentPadding = contentPadding,
            colors = colors ?: ButtonDefaults.textButtonColors(),
            content = content,
        )
    }
}
