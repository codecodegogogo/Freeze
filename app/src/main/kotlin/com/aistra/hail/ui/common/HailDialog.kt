package com.aistra.hail.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

data class HailDialogColors(
    val panel: Color,
    val field: Color,
    val fieldBorder: Color,
    val option: Color,
    val selected: Color,
    val onSelected: Color,
    val text: Color,
    val secondary: Color,
    val handle: Color,
    val primaryButton: Color,
    val onPrimaryButton: Color,
    val secondaryButton: Color,
    val onSecondaryButton: Color,
    val error: Color,
)

@Composable
fun rememberHailDialogColors(): HailDialogColors {
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) DarkHailDialogColors else LightHailDialogColors
}

private val DarkHailDialogColors = HailDialogColors(
    panel = Color(0xFF17171A),
    field = Color(0xFF242427),
    fieldBorder = Color(0xFF2E2E33),
    option = Color(0xFF242427),
    selected = Color(0xFFF4F4F5),
    onSelected = Color(0xFF111114),
    text = Color(0xFFF2F2F4),
    secondary = Color(0xFF8A8A90),
    handle = Color(0xFF4A4A50),
    primaryButton = Color(0xFFF4F4F5),
    onPrimaryButton = Color(0xFF111114),
    secondaryButton = Color(0xFF242427),
    onSecondaryButton = Color(0xFFF2F2F4),
    error = Color(0xFFF12522),
)

private val LightHailDialogColors = HailDialogColors(
    panel = Color(0xFFF4F5F7),
    field = Color(0xFFE9EAEE),
    fieldBorder = Color(0xFFDCDDE2),
    option = Color(0xFFE9EAEE),
    selected = Color(0xFF17171A),
    onSelected = Color(0xFFFFFFFF),
    text = Color(0xFF17171A),
    secondary = Color(0xFF74767E),
    handle = Color(0xFFC4C8D2),
    primaryButton = Color(0xFF17171A),
    onPrimaryButton = Color(0xFFFFFFFF),
    secondaryButton = Color(0xFFE4E5E9),
    onSecondaryButton = Color(0xFF17171A),
    error = Color(0xFFE94634),
)

/**
 * Floating dialog used by the tag, filter and group pickers. The dark palette
 * mirrors the reference design and is inverted for the light theme.
 */
@Composable
fun HailDialog(
    title: String?,
    onDismissRequest: () -> Unit,
    showHandle: Boolean = true,
    titleAlign: TextAlign = TextAlign.Center,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = rememberHailDialogColors()
    WindowDialog(
        show = true,
        title = null,
        backgroundColor = colors.panel,
        cornerRadius = 28.dp,
        outsideMargin = DpSize(0.dp, 0.dp),
        insideMargin = DpSize(20.dp, 20.dp),
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showHandle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.handle),
                    )
                }
            }
            if (title != null) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = titleAlign,
                )
                Spacer(Modifier.height(18.dp))
            }
            content()
        }
    }
}

@Composable
fun HailDialogLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberHailDialogColors()
    Text(
        text = text,
        modifier = modifier,
        color = colors.secondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun HailDialogTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberHailDialogColors()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(colors.field)
            .border(1.dp, colors.fieldBorder, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.text.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.secondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(colors.text),
                singleLine = true,
            )
        }
        if (value.text.isNotEmpty()) {
            val hapticClear = rememberHapticClick { onValueChange(TextFieldValue("")) }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = hapticClear,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Rounded-rect option used for the tag working mode: the selected row is white
 * with a check mark, the remaining options sit in a two-column grid.
 */
@Composable
fun HailDialogOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCheck: Boolean = false,
) {
    val colors = rememberHailDialogColors()
    val hapticClick = rememberHapticClick(onClick)
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(if (selected) colors.selected else colors.option)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (selected) colors.onSelected else colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected && showCheck) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.onSelected,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Capsule chip used for sort/filter choices, tag selection and group picking.
 */
@Composable
fun HailDialogChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingCheck: Boolean = false,
) {
    val colors = rememberHailDialogColors()
    val hapticClick = rememberHapticClick(onClick)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.selected else colors.option)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected && leadingCheck) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.onSelected,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = if (selected) colors.onSelected else colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Dashed "new group" chip shown after the existing groups.
 */
@Composable
fun HailDialogAddChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberHailDialogColors()
    val hapticClick = rememberHapticClick(onClick)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .drawBehind {
                drawRoundRect(
                    color = colors.fieldBorder,
                    cornerRadius = CornerRadius(size.height / 2f),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                        ),
                    ),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = colors.secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun HailDialogButtonRow(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HailDialogButton(
            text = cancelText,
            onClick = onCancel,
            primary = false,
            modifier = Modifier.weight(1f),
        )
        HailDialogButton(
            text = confirmText,
            onClick = onConfirm,
            primary = true,
            modifier = Modifier.weight(1f),
            enabled = confirmEnabled,
        )
    }
}

@Composable
private fun HailDialogButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = rememberHailDialogColors()
    val hapticClick = rememberHapticClick(onClick)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(
                if (primary) {
                    colors.primaryButton.copy(alpha = if (enabled) 1f else 0.4f)
                } else {
                    colors.secondaryButton
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = hapticClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) colors.onPrimaryButton else colors.onSecondaryButton,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun HailDialogWideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberHailDialogColors()
    val hapticClick = rememberHapticClick(onClick)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.secondaryButton)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colors.onSecondaryButton,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun HailDialogTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val colors = rememberHailDialogColors()
    val hapticClick = rememberHapticClick(onClick)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(if (destructive) colors.option else colors.field)
            .border(
                width = 1.dp,
                color = if (destructive) colors.error.copy(alpha = 0.55f) else colors.fieldBorder,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (destructive) colors.error else colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
