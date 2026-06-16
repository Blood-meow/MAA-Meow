package com.aliothmoon.maameow.ui.component.bridge

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * UI Components Bridge Layer
 *
 * Provides a unified API that abstracts over miuix and Material3 components.
 * Miuix view files should import from this package instead of directly using
 * miuix or Material3 components with incompatible APIs.
 */

// ============================================================
// TextField
// ============================================================

/**
 * Unified TextField. Miuix 0.9.2 TextField's `label` is a plain `String`, so
 * callers should pass `label = stringResource(R.string.xxx)`.
 * `placeholder` is forwarded as `useLabelAsPlaceholder = true` when label is null.
 * `readOnly` is forwarded to MiuixTextField.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = MiuixTheme.textStyles.body1,
) {
    val textFieldValue = remember(value) { androidx.compose.ui.text.input.TextFieldValue(value) }
    val effectiveLabel = label ?: placeholder ?: ""
    val useLabelAsPlaceholder = label == null && placeholder != null
    MiuixTextField(
        value = textFieldValue,
        onValueChange = { tfv -> onValueChange(tfv.text) },
        modifier = modifier,
        label = effectiveLabel,
        useLabelAsPlaceholder = useLabelAsPlaceholder,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
        trailingIcon = trailingIcon,
        textStyle = textStyle,
    )
}

// ============================================================
// TextButton (Material3 fallback - miuix TextButton has incompatible API)
// ============================================================

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

// ============================================================
// Chips (Miuix SDK has no Chip equivalent; delegate to Material3)
// ============================================================

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.SelectableChipColors =
        androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = MiuixTheme.colorScheme.primary,
            selectedLabelColor = MiuixTheme.colorScheme.onPrimary,
        ),
) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        colors = colors,
    )
}

@Composable
fun AppInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    androidx.compose.material3.InputChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun AppAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = label,
        modifier = modifier,
        leadingIcon = leadingIcon,
    )
}

// ============================================================
// SegmentedButton (Miuix SDK has no SegmentedButton equivalent)
// ============================================================

// ============================================================
// DatePicker (Miuix SDK has no DatePicker equivalent)
// ============================================================

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    datePickerState: androidx.compose.material3.DatePickerState,
) {
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    ) {
        androidx.compose.material3.DatePicker(state = datePickerState)
    }
}
