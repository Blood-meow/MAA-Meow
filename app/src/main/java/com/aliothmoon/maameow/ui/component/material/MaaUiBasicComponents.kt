package com.aliothmoon.maameow.ui.component.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.ui.isMiuixUi
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ── Text (String) ────────────────────────────────────────────

@Composable
fun MaaUiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = if (isMiuixUi) MiuixTheme.textStyles.body2
        else androidx.compose.material3.MaterialTheme.typography.bodyMedium,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Text(
            text = text, modifier = modifier, color = color,
            fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight,
            fontFamily = fontFamily, letterSpacing = letterSpacing,
            textDecoration = textDecoration, textAlign = textAlign,
            lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
            maxLines = maxLines, minLines = minLines, style = style,
        )
    } else {
        androidx.compose.material3.Text(
            text = text, modifier = modifier, color = color,
            fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight,
            fontFamily = fontFamily, letterSpacing = letterSpacing,
            textDecoration = textDecoration, textAlign = textAlign,
            lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
            maxLines = maxLines, minLines = minLines, style = style,
        )
    }
}

// ── Text (AnnotatedString) ───────────────────────────────────

@Composable
fun MaaUiText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = if (isMiuixUi) MiuixTheme.textStyles.body2
        else androidx.compose.material3.MaterialTheme.typography.bodyMedium,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.Text(
            text = text, modifier = modifier, color = color,
            fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight,
            fontFamily = fontFamily, letterSpacing = letterSpacing,
            textDecoration = textDecoration, textAlign = textAlign,
            lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
            maxLines = maxLines, minLines = minLines, style = style,
        )
    } else {
        androidx.compose.material3.Text(
            text = text, modifier = modifier, color = color,
            fontSize = fontSize, fontStyle = fontStyle, fontWeight = fontWeight,
            fontFamily = fontFamily, letterSpacing = letterSpacing,
            textDecoration = textDecoration, textAlign = textAlign,
            lineHeight = lineHeight, overflow = overflow, softWrap = softWrap,
            maxLines = maxLines, minLines = minLines, style = style,
        )
    }
}

// ── IconButton ───────────────────────────────────────────────

@Composable
fun MaaUiIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.IconButtonColors? = null,
    content: @Composable () -> Unit,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.IconButton(
            onClick = onClick, modifier = modifier, enabled = enabled, content = content,
        )
    } else {
        if (colors != null) {
            androidx.compose.material3.IconButton(
                onClick = onClick, modifier = modifier, enabled = enabled, colors = colors, content = content,
            )
        } else {
            androidx.compose.material3.IconButton(
                onClick = onClick, modifier = modifier, enabled = enabled, content = content,
            )
        }
    }
}

// ── CircularProgressIndicator ────────────────────────────────

@Composable
fun MaaUiCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 2.dp,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.CircularProgressIndicator(
            modifier = modifier, strokeWidth = strokeWidth,
        )
    } else {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = modifier, color = color, strokeWidth = strokeWidth,
        )
    }
}

// ── HorizontalDivider ────────────────────────────────────────

@Composable
fun MaaUiHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = Color.Unspecified,
) {
    if (isMiuixUi) {
        top.yukonga.miuix.kmp.basic.HorizontalDivider(
            modifier = modifier,
            thickness = thickness,
            color = if (color != Color.Unspecified) color else MiuixTheme.colorScheme.dividerLine,
        )
    } else {
        androidx.compose.material3.HorizontalDivider(
            modifier = modifier,
            thickness = thickness,
            color = if (color != Color.Unspecified) color else androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

// ── Surface ──────────────────────────────────────────────────

@Composable
fun MaaUiSurface(
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    color: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (isMiuixUi) {
        val mod = if (onClick != null) modifier.clickable { onClick() } else modifier
        top.yukonga.miuix.kmp.basic.Surface(
            modifier = mod,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content,
        )
    } else {
        if (onClick != null) {
            androidx.compose.material3.Surface(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                color = color,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
                border = border,
                content = content,
            )
        } else {
            androidx.compose.material3.Surface(
                modifier = modifier,
                shape = shape,
                color = color,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation,
                border = border,
                content = content,
            )
        }
    }
}
