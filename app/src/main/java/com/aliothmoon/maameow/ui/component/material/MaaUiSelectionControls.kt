package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.ui.isMiuixUi

/**
 * Auto-switching RadioButton: uses Miuix style when isMiuixUi, Material3 otherwise.
 * Material3 and Miuix RadioButton have the same core API (selected, onClick, modifier, enabled).
 */
@Composable
fun MaaUiRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

/**
 * Auto-switching Checkbox: uses Miuix style when isMiuixUi, Material3 otherwise.
 */
@Composable
fun MaaUiCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

/**
 * Auto-switching Switch: uses Miuix style when isMiuixUi, Material3 otherwise.
 */
@Composable
fun MaaUiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    } else {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
    }
}

/**
 * Auto-switching Slider: uses Miuix style when isMiuixUi, Material3 otherwise.
 * Note: Miuix Slider doesn't support `thumb` or `track` custom slots.
 */
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
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    } else {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}
/**
 * Auto-switching Button: uses Miuix style when isMiuixUi, Material3 otherwise.
 * Material3 Button uses shape=, Miuix uses cornerRadius=.
 */
@Composable
fun MaaUiButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable () -> Unit,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            insideMargin = contentPadding,
            content = content,
        )
    } else {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/**
 * Auto-switching OutlinedButton.
 */
@Composable
fun MaaUiOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable () -> Unit,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            insideMargin = contentPadding,
            content = content,
        )
    } else {
        androidx.compose.material3.OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/**
 * Auto-switching TextButton.
 * Miuix TextButton takes text as param, so we fall back to Miuix Button for composable content.
 */
@Composable
fun MaaUiTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable () -> Unit,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            insideMargin = contentPadding,
            content = content,
        )
    } else {
        androidx.compose.material3.TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            contentPadding = contentPadding,
            content = content,
        )
    }
}
