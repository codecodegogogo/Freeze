package com.aistra.hail.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * One card in the app detail sheet: a title, a short description and an icon.
 */
data class HailDetailAction(
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * App detail sheet shown after long-pressing an app: a large icon header with
 * an info button, an optional launch button, and a two-column action grid.
 */
@Composable
fun HailAppDetailSheet(
    info: AppInfo,
    launchTitle: String,
    onLaunch: (() -> Unit)?,
    onInfo: () -> Unit,
    actions: List<HailDetailAction>,
    onDismissRequest: () -> Unit,
) {
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val colors = if (dark) DarkAppDetailColors else LightAppDetailColors
    WindowBottomSheet(
        show = true,
        title = null,
        startAction = {
            HailAppDetailHeader(
                info = info,
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp),
            )
        },
        endAction = {
            HailAppDetailInfoButton(colors = colors, onClick = onInfo)
        },
        backgroundColor = colors.sheet,
        dragHandleColor = colors.handle,
        outsideMargin = DpSize(0.dp, 0.dp),
        insideMargin = DpSize(20.dp, 0.dp),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            if (onLaunch != null) {
                Spacer(Modifier.height(16.dp))
                HailDetailLaunchButton(
                    text = launchTitle,
                    colors = colors,
                    onClick = onLaunch,
                )
            }
            Spacer(Modifier.height(10.dp))
            HailDetailActionGrid(actions = actions, colors = colors)
        }
    }
}

@Composable
private fun HailAppDetailHeader(
    info: AppInfo,
    colors: AppDetailColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            appInfo = info,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)),
            size = 52.dp,
            showContainer = false,
            contentPadding = 0.dp,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = info.name.toString(),
            modifier = Modifier.weight(1f),
            color = colors.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HailAppDetailInfoButton(
    colors: AppDetailColors,
    onClick: () -> Unit,
) {
    val hapticInfo = rememberHapticClick(onClick)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.infoBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticInfo,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = stringResource(R.string.action_details),
            tint = colors.infoIcon,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun HailDetailLaunchButton(
    text: String,
    colors: AppDetailColors,
    onClick: () -> Unit,
) {
    val hapticClick = rememberHapticClick(onClick)
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(colors.primary)
            .border(1.dp, colors.primaryBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Launch,
            contentDescription = null,
            tint = colors.title,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = colors.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HailDetailActionGrid(
    actions: List<HailDetailAction>,
    colors: AppDetailColors,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowActions.forEach { action ->
                    HailDetailActionCard(
                        action = action,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HailDetailActionCard(
    action: HailDetailAction,
    colors: AppDetailColors,
    modifier: Modifier = Modifier,
) {
    val hapticClick = rememberHapticClick(action.onClick)
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.iconContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = colors.title,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = action.title,
                color = colors.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = action.summary,
                color = colors.summary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class AppDetailColors(
    val sheet: Color,
    val card: Color,
    val cardBorder: Color,
    val primary: Color,
    val primaryBorder: Color,
    val iconContainer: Color,
    val title: Color,
    val summary: Color,
    val handle: Color,
    val infoBackground: Color,
    val infoIcon: Color,
)

private val DarkAppDetailColors = AppDetailColors(
    sheet = Color(0xFF101217),
    card = Color(0xFF16171B),
    cardBorder = Color(0xFF24252B),
    primary = Color(0xFF232328),
    primaryBorder = Color(0xFF303137),
    iconContainer = Color(0xFF232429),
    title = Color(0xFFF3F4F6),
    summary = Color(0xFF85868D),
    handle = Color(0xFFAFC5E8),
    infoBackground = Color(0xFF202329),
    infoIcon = Color(0xFFB7BFCC),
)

private val LightAppDetailColors = AppDetailColors(
    sheet = Color(0xFFF4F5F7),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE4E5E9),
    primary = Color(0xFFECEDF1),
    primaryBorder = Color(0xFFDCDDE2),
    iconContainer = Color(0xFFEEF0F3),
    title = Color(0xFF1A1B1F),
    summary = Color(0xFF74767E),
    handle = Color(0xFF8A94A6),
    infoBackground = Color(0xFFE9EBEF),
    infoIcon = Color(0xFF5C626E),
)
