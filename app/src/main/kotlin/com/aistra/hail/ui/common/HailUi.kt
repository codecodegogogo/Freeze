@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aistra.hail.ui.common

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.HailRoute
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val view = LocalView.current
    val currentOnClick by rememberUpdatedState(onClick)
    return remember(view) {
        {
            if (HailData.hapticFeedback) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            currentOnClick()
        }
    }
}

/**
 * A floating toast that stays visible while [visible] is true.
 * Unlike [HUI.showToast] it is part of the UI tree, so it can follow a long
 * running operation and disappear as soon as the operation finishes.
 */
@Composable
fun HailToast(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.92f),
        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f),
    ) {
        Card(
            modifier = Modifier.shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false,
            ),
            cornerRadius = 22.dp,
            insideMargin = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceContainerHighest,
                contentColor = MiuixTheme.colorScheme.onSurfaceContainerHighest,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    size = 16.dp,
                    strokeWidth = 2.dp,
                )
                Text(
                    text = text,
                    color = MiuixTheme.colorScheme.onSurfaceContainerHighest,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}

data class HailAction(
    val title: String,
    val icon: ImageVector?,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun HailActionGrid(
    actions: List<HailAction>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actions.chunked(columns.coerceAtLeast(1)).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowActions.forEach { action ->
                    HailActionButton(
                        action = action,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowActions.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HailActionButton(
    action: HailAction,
    modifier: Modifier = Modifier,
) {
    val hapticClick = rememberHapticClick(action.onClick)
    val contentColor = if (action.destructive) {
        MiuixTheme.colorScheme.error
    } else {
        MiuixTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (action.destructive) {
                    MiuixTheme.colorScheme.error.copy(alpha = 0.10f)
                } else {
                    MiuixTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        action.icon?.let { icon ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (action.destructive) {
                            MiuixTheme.colorScheme.error.copy(alpha = 0.12f)
                        } else {
                            MiuixTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(11.dp))
        }
        Text(
            text = action.title,
            modifier = Modifier.weight(1f),
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (action.icon == null) TextAlign.Center else TextAlign.Start,
        )
    }
}

@Composable
fun HailSelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
    maxWidth: Dp? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val hapticClick = rememberHapticClick(onClick)
    val background by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.onSurface
        } else {
            MiuixTheme.colorScheme.surfaceContainerHigh
        },
        label = "pillBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.surface
        } else {
            MiuixTheme.colorScheme.onSurface
        },
        label = "pillForeground",
    )
    Box(
        modifier = modifier
            .height(height)
            .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
            .clip(RoundedCornerShape(50))
            .background(background)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = hapticClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun HailScreenScaffold(
    title: String,
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    compactTopBar: Boolean = false,
    denseTopBar: Boolean = false,
    denseLargeTitle: Boolean = false,
    showUnavailableQuickAction: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    trailingAction: (@Composable () -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showRail = maxWidth >= 840.dp
        val expandRail = maxWidth >= 1200.dp
        val hapticBackClick = if (onBack != null) rememberHapticClick(onBack) else null
        val floatingNavigationBar = rememberBooleanPreference(
            HailData.FLOATING_NAVIGATION_BAR,
            HailData.DEFAULT_FLOATING_NAVIGATION_BAR,
        ).first.value
        val actionButtonInBottomBar = rememberStringPreference(
            HailData.ACTION_BUTTON_POSITION,
            HailData.DEFAULT_ACTION_BUTTON_POSITION,
        ).first.value == HailData.ACTION_BUTTON_BOTTOM_BAR
        val unavailableQuickAction: (@Composable () -> Unit)? =
            if (showUnavailableQuickAction && actionButtonInBottomBar && !showRail) {
                {
                    HailQuickActionButton(
                        onClick = { HUI.showToast(R.string.action_unavailable_on_page) },
                        showBackground = false,
                        size = if (floatingNavigationBar) 52.dp else 36.dp,
                        contentColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        pressedContentColor = MiuixTheme.colorScheme.onSurface,
                        contentDescription = stringResource(R.string.action_open_operation_menu),
                    )
                }
            } else {
                null
            }
        val effectiveTrailingAction = trailingAction ?: unavailableQuickAction
        Scaffold(
            topBar = {
                if (denseTopBar) {
                    HailDenseTopAppBar(
                        title = title,
                        subtitle = subtitle,
                        titlePadding = if (onBack == null) 26.dp else 8.dp,
                        largeTitle = denseLargeTitle,
                        navigationIcon = {
                            if (hapticBackClick != null) {
                                IconButton(onClick = hapticBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        actions = actions,
                    )
                } else if (compactTopBar) {
                    SmallTopAppBar(
                        title = title,
                        subtitle = subtitle,
                        navigationIcon = {
                            if (hapticBackClick != null) {
                                IconButton(onClick = hapticBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        actions = actions,
                    )
                } else {
                    TopAppBar(
                        title = title,
                        subtitle = subtitle,
                        navigationIcon = {
                            if (hapticBackClick != null) {
                                IconButton(onClick = hapticBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        actions = actions,
                    )
                }
            },
            bottomBar = {
                if (!showRail) {
                    if (floatingNavigationBar) {
                        HailFloatingNavigationBar(
                            route = route,
                            onNavigate = onNavigate,
                            trailingAction = effectiveTrailingAction,
                        )
                    } else {
                        HailNavigationBar(
                            route = route,
                            onNavigate = onNavigate,
                            trailingAction = effectiveTrailingAction,
                        )
                    }
                }
            },
            floatingActionButton = if (showRail) {
                effectiveTrailingAction ?: floatingActionButton
            } else {
                floatingActionButton
            },
        ) { innerPadding ->
            if (showRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    HailNavigationRail(
                        route = route,
                        onNavigate = onNavigate,
                        expanded = expandRail,
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        content(innerPadding)
                    }
                }
            } else {
                content(innerPadding)
            }
        }
    }
}

@Composable
fun HailQuickActionButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    backgroundColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MiuixTheme.colorScheme.onSurface,
    pressedContentColor: Color = contentColor,
    elevation: Dp = 0.dp,
    rotation: Float = 0f,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hapticClick = if (onClick != null) rememberHapticClick(onClick) else null
    val pressColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation = elevation, shape = shape)
                } else {
                    Modifier
                },
            )
            .size(size)
            .clip(shape)
            .then(
                if (showBackground) {
                    Modifier.background(backgroundColor)
                } else {
                    Modifier
                },
            )
            .background(
                if (isPressed && showBackground) pressColor else Color.Transparent,
            )
            .then(
                if (hapticClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = hapticClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AcUnit,
            contentDescription = contentDescription,
            tint = if (isPressed) pressedContentColor else contentColor,
            modifier = Modifier
                .size(iconSize)
                .rotate(rotation),
        )
    }
}

@Composable
private fun HailDenseTopAppBar(
    title: String,
    subtitle: String,
    titlePadding: Dp,
    largeTitle: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val contentHeight = when {
        largeTitle && subtitle.isNotEmpty() -> 64.dp
        largeTitle -> 52.dp
        subtitle.isNotEmpty() -> 52.dp
        else -> 40.dp
    }
    val titleStyle = if (largeTitle) {
        MiuixTheme.textStyles.title1
    } else {
        MiuixTheme.textStyles.title3
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiuixTheme.colorScheme.surface)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = titlePadding, end = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = if (largeTitle) FontWeight.Normal else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
private fun HailFloatingNavigationBar(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    trailingAction: (@Composable () -> Unit)?,
) {
    val navigateHome = rememberHapticClick { onNavigate(HailRoute.Home) }
    val navigateSettings = rememberHapticClick { onNavigate(HailRoute.Settings) }
    FloatingNavigationBar {
        FloatingNavigationBarItem(
            selected = route == HailRoute.Home,
            onClick = navigateHome,
            icon = Icons.Outlined.Home,
            label = stringResource(R.string.title_home),
            modifier = Modifier.width(64.dp),
        )
        FloatingNavigationBarItem(
            selected = route == HailRoute.Settings,
            onClick = navigateSettings,
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.title_settings),
            modifier = Modifier.width(64.dp),
        )
        if (trailingAction != null) {
            Box(
                modifier = Modifier.width(64.dp).height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                trailingAction()
            }
        }
    }
}

@Composable
private fun HailNavigationBar(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    trailingAction: (@Composable () -> Unit)?,
) {
    val navigateHome = rememberHapticClick { onNavigate(HailRoute.Home) }
    val navigateSettings = rememberHapticClick { onNavigate(HailRoute.Settings) }
    NavigationBar {
        NavigationBarItem(
            selected = route == HailRoute.Home,
            onClick = navigateHome,
            icon = Icons.Outlined.Home,
            label = stringResource(R.string.title_home),
        )
        NavigationBarItem(
            selected = route == HailRoute.Settings,
            onClick = navigateSettings,
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.title_settings),
        )
        if (trailingAction != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(NavigationBarDefaults.ItemHeight),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp),
                ) {
                    trailingAction()
                }
                Text(
                    text = stringResource(R.string.action_operation),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HailNavigationRail(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    expanded: Boolean,
) {
    val navigateHome = rememberHapticClick { onNavigate(HailRoute.Home) }
    val navigateSettings = rememberHapticClick { onNavigate(HailRoute.Settings) }
    NavigationRail(
        expanded = expanded,
        modifier = Modifier.fillMaxHeight(),
    ) {
        NavigationRailItem(
            selected = route == HailRoute.Home,
            onClick = navigateHome,
            icon = Icons.Outlined.Home,
            label = stringResource(R.string.title_home),
        )
        NavigationRailItem(
            selected = route == HailRoute.Settings,
            onClick = navigateSettings,
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.title_settings),
        )
    }
}

@Composable
fun HailGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            SmallTitle(text = title)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            insideMargin = PaddingValues(vertical = 4.dp),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceContainer,
            ),
            content = { content() },
        )
    }
}

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

private val GrayIconColorFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0f) },
)

@Composable
fun AppIcon(
    info: ApplicationInfo?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    packageName: String? = null,
    showContainer: Boolean = true,
    contentPadding: Dp = 4.dp,
    uninstalled: Boolean = false,
    reloadToken: Any? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }
    val iconPackageName = info?.packageName ?: packageName
    val userId = HPackages.myUserId
    var bitmap by remember(iconPackageName, sizePx, reloadToken) {
        mutableStateOf(
            iconPackageName?.let {
                AppIconCache.getCachedBitmap(it, userId, sizePx)
            },
        )
    }
    LaunchedEffect(iconPackageName, sizePx, uninstalled, reloadToken) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val name = iconPackageName ?: return@runCatching null
                val applicationInfo = info ?: HPackages.getApplicationInfoOrNull(name)
                val loaded = if (!uninstalled && applicationInfo != null) {
                    AppIconCache.getOrLoadBitmap(context, applicationInfo, userId, sizePx)
                } else {
                    null
                }
                loaded ?: AppIconCache.getPersistedBitmap(context, name, userId, sizePx)
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showContainer) {
                    Modifier.background(MiuixTheme.colorScheme.surfaceContainerHigh, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { image ->
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                colorFilter = if (uninstalled) GrayIconColorFilter else null,
            )
        }
    }
}

@Composable
fun AppIcon(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showContainer: Boolean = true,
    contentPadding: Dp = 4.dp,
    uninstalled: Boolean = false,
    reloadToken: Any? = null,
) = AppIcon(
    info = appInfo.applicationInfo,
    modifier = modifier,
    size = size,
    packageName = appInfo.packageName,
    showContainer = showContainer,
    contentPadding = contentPadding,
    uninstalled = uninstalled,
    reloadToken = reloadToken,
)
