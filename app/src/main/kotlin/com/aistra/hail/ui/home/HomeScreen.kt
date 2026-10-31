@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.aistra.hail.ui.home

import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.common.AppIcon
import com.aistra.hail.ui.common.EmptyState
import com.aistra.hail.ui.common.HailAction
import com.aistra.hail.ui.common.HailActionGrid
import com.aistra.hail.ui.common.HailAppDetailSheet
import com.aistra.hail.ui.common.HailDetailAction
import com.aistra.hail.ui.common.HailDialog
import com.aistra.hail.ui.common.HailDialogButtonRow
import com.aistra.hail.ui.common.HailDialogChip
import com.aistra.hail.ui.common.HailDialogLabel
import com.aistra.hail.ui.common.HailDialogOption
import com.aistra.hail.ui.common.HailDialogTextButton
import com.aistra.hail.ui.common.HailDialogTextField
import com.aistra.hail.ui.common.HailQuickActionButton
import com.aistra.hail.ui.common.HailScreenScaffold
import com.aistra.hail.ui.common.HailSelectablePill
import com.aistra.hail.ui.common.rememberBooleanPreference
import com.aistra.hail.ui.common.rememberHailDialogColors
import com.aistra.hail.ui.common.rememberHapticClick
import com.aistra.hail.ui.common.rememberStringPreference
import com.aistra.hail.ui.main.HailRoute
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.PinyinSearch
import java.text.Collator
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class HomeMessage(
    val title: String? = null,
    val message: String,
)

private data class HomeAppEntry(
    val info: AppInfo,
    val label: String,
    val uninstalled: Boolean,
)

private val AppGridItemTopPadding = 8.dp

@Composable
fun HomeScreen(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val actionButtonInBottomBar = rememberStringPreference(
        HailData.ACTION_BUTTON_POSITION,
        HailData.DEFAULT_ACTION_BUTTON_POSITION,
    ).first.value == HailData.ACTION_BUTTON_BOTTOM_BAR
    val floatingNavigationBar = rememberBooleanPreference(
        HailData.FLOATING_NAVIGATION_BAR,
        HailData.DEFAULT_FLOATING_NAVIGATION_BAR,
    ).first.value
    val actionButtonInNormalBar = actionButtonInBottomBar && !floatingNavigationBar
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTagIndex by rememberSaveable { mutableIntStateOf(0) }
    var multiselect by remember { mutableStateOf(false) }
    var actionMenuExpanded by remember { mutableStateOf(false) }
    var dataVersion by remember { mutableIntStateOf(0) }
    var tagVersion by remember { mutableIntStateOf(0) }
    var actionApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectionSheet by remember { mutableStateOf(false) }
    var tagEditorVisible by remember { mutableStateOf(false) }
    var tagEditorTagId by remember { mutableStateOf<Int?>(null) }
    var tagEditorTargets by remember { mutableStateOf<List<AppInfo>?>(null) }
    var tagEditorNew by remember { mutableStateOf(false) }
    var tagEditorName by remember { mutableStateOf(TextFieldValue()) }
    var tagEditorAction by remember { mutableIntStateOf(0) }
    var tagEditorSelections by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var tagEditorError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<HomeMessage?>(null) }
    var actionMenuBounds by remember { mutableStateOf<Rect?>(null) }
    var allAppsOperationInProgress by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<AppInfo>() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        dataVersion++
    }
    val popupGap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val popupPositionProvider = remember(actionMenuBounds, popupGap) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val bounds = actionMenuBounds ?: return IntOffset.Zero
                val x = (bounds.center.x - popupContentSize.width / 2f)
                    .roundToInt()
                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val y = (bounds.top - popupContentSize.height - popupGap)
                    .roundToInt()
                    .coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }

    val tags = remember(tagVersion) { HailData.tags.toList() }
    val safeTagIndex = selectedTagIndex.coerceIn(0, (tags.size - 1).coerceAtLeast(0))
    val currentTag = tags.getOrNull(safeTagIndex)
    val currentTagWorkingMode = currentTag?.let { HailData.tagWorkingMode(it.second) } ?: HailData.workingMode

    val homeApps = remember(dataVersion, tagVersion) {
        val packageManager = context.packageManager
        val collator = Collator.getInstance()
        HailData.checkedList.map { info ->
            val applicationInfo = info.applicationInfo
            val uninstalled = HPackages.isAppUninstalled(info.packageName)
            HomeAppEntry(
                info = info,
                label = applicationInfo
                    ?.takeUnless { uninstalled }
                    ?.loadLabel(packageManager)
                    ?.toString()
                    ?: info.packageName,
                uninstalled = uninstalled,
            )
        }.sortedWith { first, second ->
            compareHomeAppOrder(
                HomeAppOrderKey(first.uninstalled, first.info.pinned, first.label),
                HomeAppOrderKey(second.uninstalled, second.info.pinned, second.label),
                collator::compare,
            )
        }
    }
    val appLabels = remember(homeApps) {
        homeApps.associate { it.info.packageName to it.label }
    }
    // AppInfo.equals only compares package names, so tagIdList mutations can leave
    // homeApps/tags equal by value. Include the explicit data versions in the key.
    val appsByTag = remember(homeApps, tags, dataVersion, tagVersion) {
        tags.associate { (_, tagId) ->
            tagId to homeApps.filter { tagId in it.info.tagIdList }.map { it.info }
        }
    }
    val pageApps = remember(appsByTag, currentTag) {
        currentTag?.let { appsByTag[it.second].orEmpty() }.orEmpty()
    }
    val searchApps = remember(query, homeApps) {
        if (query.isBlank()) {
            emptyList()
        } else {
            homeApps.map { it.info }.filter {
                val label = appLabels[it.packageName].orEmpty()
                FuzzySearch.search(it.packageName, query) ||
                    FuzzySearch.search(label, query) ||
                    PinyinSearch.searchPinyinAll(label, query)
            }
        }
    }
    val visibleApps = if (query.isBlank()) pageApps else searchApps

    fun refresh() {
        dataVersion++
        scope.launch {
            delay(350)
            dataVersion++
        }
    }

    fun removeFromHome(info: AppInfo, save: Boolean = true) {
        HailData.removeCheckedApp(info.packageName, save)
        if (save) refresh()
    }

    fun launchApp(info: AppInfo) {
        if (multiselect) {
            if (info in selected) selected.remove(info) else selected.add(info)
            return
        }
        if (HPackages.isAppUninstalled(info.packageName)) {
            message = HomeMessage(
                message = resources.getString(R.string.app_not_installed),
            )
            return
        }
        if (AppManager.isAppFrozen(info.packageName, currentTagWorkingMode) &&
            AppManager.setAppFrozen(info.packageName, false, currentTagWorkingMode)
        ) {
            refresh()
        }
        if (currentTagWorkingMode == HailData.MODE_ISLAND_HIDE) {
            HIsland.ensureLaunchIntentExists(info.packageName)
        }
        app.packageManager.getLaunchIntentForPackage(info.packageName)?.let {
            context.startActivity(it)
        } ?: HUI.showToast(R.string.activity_not_found)
    }

    fun canOperateList(workingMode: String): Boolean {
        if (workingMode == HailData.MODE_DEFAULT) {
            message = HomeMessage(message = resources.getString(R.string.msg_guide))
            return false
        }
        if (workingMode == HailData.MODE_SHIZUKU_HIDE) {
            runCatching { HShizuku.isRoot }.onSuccess {
                if (!it) {
                    message = HomeMessage(message = resources.getString(R.string.shizuku_hide_adb))
                    return false
                }
            }
        }
        return true
    }

    suspend fun executeListFrozen(
        frozen: Boolean,
        list: List<AppInfo>,
        workingMode: String,
    ): String? = withContext(Dispatchers.IO) {
        val filtered = list.filter {
            AppManager.isAppFrozen(it.packageName, workingMode) != frozen ||
                (!frozen && HailData.hasAppOperation(it.packageName, workingMode))
        }
        AppManager.setListFrozen(
            frozen,
            workingMode,
            *filtered.toTypedArray(),
        )
    }

    fun setListFrozen(
        frozen: Boolean,
        list: List<AppInfo>,
        workingMode: String = currentTagWorkingMode,
        update: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        if (!canOperateList(workingMode)) {
            onComplete?.invoke(false)
            return
        }
        val snapshot = list.toList()
        scope.launch {
            val result = runCatching {
                executeListFrozen(frozen, snapshot, workingMode)
            }.getOrNull()
            if (result == null) {
                HUI.showToast(R.string.permission_denied)
                onComplete?.invoke(false)
                return@launch
            }
            if (update) refresh()
            HUI.showToast(workingMode.operationMessageId(frozen), result)
            onComplete?.invoke(true)
        }
    }

    fun setAllAppsFrozen(frozen: Boolean) {
        if (allAppsOperationInProgress) return
        val groups = HailData.checkedList.groupBy(HailData::appWorkingModeTag).map { (tagMode, apps) ->
            val (tagId, workingMode) = tagMode
            val groupName = tagId?.let { id ->
                tags.firstOrNull { it.second == id }?.first?.let(::tagDisplayName)
            } ?: resources.getString(R.string.default_working_mode)
            Triple(workingMode, groupName, apps.toList())
        }
        if (groups.isEmpty()) return
        allAppsOperationInProgress = true
        HUI.showToast(R.string.msg_operation_in_progress)
        scope.launch {
            var guide = false
            var shizukuHideNeedsRoot = false
            var denied = false
            val messages = withContext(Dispatchers.IO) {
                groups.mapNotNull { (workingMode, groupName, apps) ->
                    when {
                        workingMode == HailData.MODE_DEFAULT -> {
                            guide = true
                            null
                        }

                        workingMode == HailData.MODE_SHIZUKU_HIDE &&
                            runCatching { !HShizuku.isRoot }.getOrDefault(true) -> {
                            shizukuHideNeedsRoot = true
                            null
                        }

                        else -> runCatching {
                            val filtered = apps.filter {
                                AppManager.isAppFrozen(it.packageName, workingMode) != frozen ||
                                    (!frozen &&
                                        HailData.hasAppOperation(it.packageName, workingMode))
                            }
                            if (filtered.isEmpty()) return@runCatching null
                            val result = AppManager.setListFrozen(
                                frozen,
                                workingMode,
                                *filtered.toTypedArray(),
                            )
                            if (result == null) {
                                denied = true
                                null
                            } else {
                                resources.getString(
                                    R.string.msg_tag_operation,
                                    groupName,
                                    resources.getString(
                                        workingMode.operationStatusId(frozen),
                                    ),
                                )
                            }
                        }.getOrElse {
                            denied = true
                            null
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                allAppsOperationInProgress = false
                refresh()
                when {
                    guide -> message = HomeMessage(
                        message = resources.getString(R.string.msg_guide),
                    )
                    shizukuHideNeedsRoot -> message = HomeMessage(
                        message = resources.getString(R.string.shizuku_hide_adb),
                    )
                    denied -> HUI.showToast(R.string.permission_denied)
                }
                if (messages.isNotEmpty()) {
                    HUI.showToast(messages.joinToString("\n"), messages.size > 1)
                }
            }
        }
    }

    fun closeTagEditor() {
        tagEditorVisible = false
        tagEditorTagId = null
        tagEditorTargets = null
        tagEditorNew = false
        tagEditorError = null
    }

    fun openTagEditor(
        targets: List<AppInfo>?,
        isNew: Boolean = false,
        tagIndex: Int = safeTagIndex,
    ) {
        val editorTag = if (!isNew && targets == null) {
            HailData.tags.getOrNull(tagIndex)
        } else {
            null
        }
        tagEditorVisible = true
        tagEditorTagId = editorTag?.second
        tagEditorTargets = targets
        tagEditorNew = isNew
        tagEditorName = TextFieldValue(
            if (!isNew && targets == null && query.isBlank()) editorTag?.first.orEmpty() else "",
        )
        tagEditorSelections = targets?.flatMap { it.tagIdList }?.toSet().orEmpty()
        tagEditorError = null
        val permission = HailData.workingPermission(HailData.workingMode)
        val actions = listOf<String?>(null) + HailData.supportedWorkingActions(permission)
        val currentAction = if (!isNew && targets == null) {
            editorTag?.second?.let(HailData::tagWorkingAction)
        } else {
            null
        }
        tagEditorAction = actions.indexOf(currentAction).coerceAtLeast(0)
    }

    fun deselect() {
        selected.clear()
        multiselect = false
        dataVersion++
    }

    val actionButton: @Composable (Boolean) -> Unit = { inBottomBar ->
        val darkTheme = MiuixTheme.colorScheme.surface.luminance() < 0.5f
        val rotation by animateFloatAsState(
            targetValue = if (actionMenuExpanded) -45f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "fabRotation",
        )
        HailQuickActionButton(
            onClick = { actionMenuExpanded = !actionMenuExpanded },
            modifier = Modifier
                .then(
                    if (inBottomBar) Modifier else Modifier.padding(end = 12.dp),
                )
                .onGloballyPositioned { actionMenuBounds = it.boundsInWindow() },
            showBackground = !inBottomBar,
            size = when {
                actionButtonInNormalBar -> 36.dp
                inBottomBar -> 52.dp
                else -> 60.dp
            },
            iconSize = if (inBottomBar) 24.dp else 28.dp,
            backgroundColor = if (inBottomBar) {
                MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
            } else if (darkTheme) {
                Color.Black
            } else {
                Color.White
            },
            contentColor = when {
                inBottomBar && !actionMenuExpanded ->
                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f)

                inBottomBar -> MiuixTheme.colorScheme.onSurface
                darkTheme -> Color.White
                else -> Color.Black
            },
            pressedContentColor = if (inBottomBar) {
                MiuixTheme.colorScheme.onSurface
            } else if (darkTheme) {
                Color.White
            } else {
                Color.Black
            },
            elevation = if (inBottomBar) 0.dp else 12.dp,
            rotation = if (inBottomBar) 0f else rotation,
            contentDescription = stringResource(
                if (actionMenuExpanded) {
                    R.string.action_close_operation_menu
                } else {
                    R.string.action_open_operation_menu
                },
            ),
        )
    }
    val selectAllClick = rememberHapticClick {
        selected.addAll(visibleApps.filterNot { it in selected })
        dataVersion++
    }
    val deselectClick = rememberHapticClick(::deselect)
    val selectionMenuClick = rememberHapticClick { selectionSheet = true }

    HailScreenScaffold(
        title = if (multiselect) {
            resources.getString(R.string.msg_selected, selected.size.toString())
        } else {
            stringResource(R.string.app_name)
        },
        route = route,
        onNavigate = onNavigate,
        actions = {
            if (multiselect) {
                IconButton(onClick = selectAllClick) {
                    Icon(Icons.Outlined.SelectAll, contentDescription = stringResource(R.string.action_select_all))
                }
                IconButton(onClick = deselectClick) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_deselect))
                }
                IconButton(onClick = selectionMenuClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
            }
        },
        trailingAction = if (actionButtonInBottomBar) {
            { actionButton(true) }
        } else {
            null
        },
        floatingActionButton = {
            if (!actionButtonInBottomBar) actionButton(false)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
            Spacer(Modifier.height(12.dp))
            SearchBar(
                inputField = {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = query.isNotEmpty(),
                        onExpandedChange = { if (!it) query = "" },
                        label = stringResource(R.string.action_search),
                    )
                },
                expanded = query.isNotEmpty(),
                onExpandedChange = { if (!it) query = "" },
            ) {}

            if (query.isBlank() && currentTag != null && !multiselect) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        tags,
                        key = { tagIndex, tag -> "$tagIndex:${tag.second}:${tag.first}" },
                    ) { index, tag ->
                        TagButton(
                            text = tag.first,
                            selected = index == safeTagIndex,
                            onClick = { selectedTagIndex = index },
                            onLongClick = {
                                selectedTagIndex = index
                                openTagEditor(null, tagIndex = index)
                            },
                        )
                    }
                    item {
                        AddTagButton(onClick = { openTagEditor(null, isNew = true) })
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            val bottomPadding = innerPadding.calculateBottomPadding()
            if (query.isBlank() && tags.isNotEmpty()) {
                val pagerState = rememberPagerState(
                    initialPage = safeTagIndex,
                    pageCount = { tags.size },
                )
                LaunchedEffect(pagerState.settledPage) {
                    if (selectedTagIndex != pagerState.settledPage) {
                        selectedTagIndex = pagerState.settledPage
                    }
                }
                LaunchedEffect(safeTagIndex, tags.size) {
                    if (tags.isNotEmpty() && pagerState.currentPage != safeTagIndex) {
                        pagerState.scrollToPage(safeTagIndex)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (actionMenuExpanded) Modifier.blur(5.dp) else Modifier,
                        ),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !multiselect,
                    ) { page ->
                        val tag = tags.getOrNull(page)
                        AppGrid(
                            apps = tag?.let { appsByTag[it.second].orEmpty() }.orEmpty(),
                            selected = selected,
                            multiselect = multiselect,
                            workingMode = tag?.let {
                                HailData.tagWorkingMode(it.second)
                            } ?: HailData.workingMode,
                            appLabels = appLabels,
                            refreshToken = dataVersion,
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = bottomPadding + 16.dp,
                            ),
                            onClick = ::launchApp,
                            onLongClick = {
                                if (multiselect) selectionSheet = true else actionApp = it
                            },
                        )
                    }
                    AppGridTopMask()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (actionMenuExpanded) Modifier.blur(5.dp) else Modifier,
                        ),
                ) {
                    AppGrid(
                        apps = visibleApps,
                        selected = selected,
                        multiselect = multiselect,
                        workingMode = currentTagWorkingMode,
                        appLabels = appLabels,
                        refreshToken = dataVersion,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = bottomPadding + 16.dp,
                        ),
                        onClick = ::launchApp,
                        onLongClick = {
                            if (multiselect) selectionSheet = true else actionApp = it
                        },
                    )
                    AppGridTopMask()
                }
            }
        }
        if (allAppsOperationInProgress) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = innerPadding.calculateTopPadding() + 12.dp),
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.msg_operation_in_progress),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
    }

    if (actionMenuExpanded) {
        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = { actionMenuExpanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FabBubbleAction(
                    text = stringResource(R.string.action_operate_current),
                    onClick = {
                        actionMenuExpanded = false
                        setListFrozen(
                            true,
                            visibleApps.filterNot { it.whitelisted },
                            currentTagWorkingMode,
                        )
                    },
                )
                FabBubbleAction(
                    text = stringResource(R.string.action_restore_current),
                    onClick = {
                        actionMenuExpanded = false
                        setListFrozen(false, visibleApps, currentTagWorkingMode)
                    },
                )
                FabBubbleAction(
                    text = stringResource(R.string.action_freeze_all),
                    onClick = {
                        actionMenuExpanded = false
                        setAllAppsFrozen(true)
                    },
                )
                FabBubbleAction(
                    text = stringResource(R.string.action_unfreeze_all),
                    onClick = {
                        actionMenuExpanded = false
                        setAllAppsFrozen(false)
                    },
                )
            }
        }
    }

    actionApp?.let { info ->
        val installed = !HPackages.isAppUninstalled(info.packageName)
        val frozen = AppManager.isAppFrozen(info.packageName, currentTagWorkingMode)
        val detailActions = buildList {
            if (installed) {
                add(
                    HailDetailAction(
                        title = stringResource(
                            if (frozen) {
                                R.string.detail_action_unfreeze
                            } else {
                                R.string.detail_action_freeze
                            },
                        ),
                        summary = stringResource(
                            if (frozen) {
                                R.string.detail_action_unfreeze_summary
                            } else {
                                R.string.detail_action_freeze_summary
                            },
                        ),
                        icon = if (frozen) Icons.Outlined.LockOpen else Icons.Outlined.AcUnit,
                    ) {
                        actionApp = null
                        setListFrozen(!frozen, listOf(info), currentTagWorkingMode)
                    },
                )
            }
            add(
                HailDetailAction(
                    title = stringResource(
                        if (info.pinned) R.string.detail_action_unpin else R.string.detail_action_pin,
                    ),
                    summary = stringResource(
                        if (info.pinned) {
                            R.string.detail_action_unpin_summary
                        } else {
                            R.string.detail_action_pin_summary
                        },
                    ),
                    icon = Icons.Outlined.PushPin,
                ) {
                    info.pinned = !info.pinned
                    HailData.saveApps()
                    actionApp = null
                    refresh()
                },
            )
            if (frozen) {
                add(
                    HailDetailAction(
                        title = stringResource(R.string.detail_action_unfreeze_remove),
                        summary = stringResource(R.string.detail_action_unfreeze_remove_summary),
                        icon = Icons.Outlined.RemoveCircleOutline,
                    ) {
                        actionApp = null
                        setListFrozen(
                            false,
                            listOf(info),
                            currentTagWorkingMode,
                            update = false,
                        ) { restored ->
                            if (restored) {
                                removeFromHome(info)
                            } else {
                                refresh()
                            }
                        }
                    },
                )
            }
            add(
                HailDetailAction(
                    title = stringResource(R.string.detail_action_tag),
                    summary = stringResource(R.string.detail_action_tag_summary),
                    icon = Icons.AutoMirrored.Outlined.Label,
                ) {
                    actionApp = null
                    openTagEditor(listOf(info))
                },
            )
            add(
                HailDetailAction(
                    title = stringResource(R.string.detail_action_multiselect),
                    summary = stringResource(R.string.detail_action_multiselect_summary),
                    icon = Icons.Outlined.GridView,
                ) {
                    actionApp = null
                    selected.clear()
                    selected.add(info)
                    multiselect = true
                    dataVersion++
                },
            )
            add(
                HailDetailAction(
                    title = stringResource(R.string.detail_action_remove),
                    summary = stringResource(R.string.detail_action_remove_summary),
                    icon = Icons.Outlined.DeleteOutline,
                ) {
                    actionApp = null
                    removeFromHome(info)
                },
            )
        }
        HailAppDetailSheet(
            info = info,
            launchTitle = stringResource(R.string.detail_action_launch),
            onLaunch = if (installed) {
                {
                    actionApp = null
                    launchApp(info)
                }
            } else {
                null
            },
            onInfo = {
                actionApp = null
                HUI.startActivity(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    HPackages.packageUri(info.packageName),
                )
            },
            actions = detailActions,
            onDismissRequest = { actionApp = null },
        )
    }

    if (selectionSheet) {
        WindowBottomSheet(
            show = true,
            title = resources.getString(R.string.msg_selected, selected.size.toString()),
            onDismissRequest = { selectionSheet = false },
        ) {
            val actions = listOf(
                HailAction(
                    title = stringResource(R.string.action_freeze),
                    icon = Icons.Outlined.AcUnit,
                ) {
                    selectionSheet = false
                    setListFrozen(true, selected.toList(), currentTagWorkingMode, false)
                    deselect()
                },
                HailAction(
                    title = stringResource(R.string.action_unfreeze),
                    icon = Icons.Outlined.LockOpen,
                ) {
                    selectionSheet = false
                    setListFrozen(false, selected.toList(), currentTagWorkingMode, false)
                    deselect()
                },
                HailAction(
                    title = stringResource(R.string.action_tag_set),
                    icon = Icons.AutoMirrored.Outlined.Label,
                ) {
                    selectionSheet = false
                    openTagEditor(selected.toList())
                },
                HailAction(
                    title = stringResource(R.string.action_remove_home),
                    icon = Icons.Outlined.DeleteOutline,
                    destructive = true,
                ) {
                    selectionSheet = false
                    selected.toList().forEach {
                        HailData.removeCheckedApp(it.packageName, false)
                    }
                    HailData.saveApps()
                    deselect()
                    refresh()
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                HailActionGrid(actions)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (tagEditorVisible) {
        val targets = tagEditorTargets
        val permission = HailData.workingPermission(HailData.workingMode)
        val supportedActions = HailData.supportedWorkingActions(permission)
        val actionValues = listOf<String?>(null) + supportedActions
        val actionEntries = listOf(stringResource(R.string.tag_working_action_default)) +
            supportedActions.map { stringResource(it.workingActionTitleId()) }
        val dialogColors = rememberHailDialogColors()
        HailDialog(
            title = stringResource(
                if (tagEditorNew) R.string.action_tag_add else R.string.action_tag_set,
            ),
            onDismissRequest = ::closeTagEditor,
        ) {
            HailDialogLabel(stringResource(R.string.tag_name_label))
            Spacer(Modifier.height(8.dp))
            HailDialogTextField(
                value = tagEditorName,
                onValueChange = {
                    tagEditorName = it
                    tagEditorError = null
                },
                placeholder = stringResource(R.string.tag_name_placeholder),
            )
            tagEditorError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = dialogColors.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (targets != null) {
                Spacer(Modifier.height(18.dp))
                HailDialogLabel(stringResource(R.string.tag))
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        val selected = tag.second in tagEditorSelections
                        HailDialogChip(
                            text = tag.first,
                            selected = selected,
                            leadingCheck = true,
                            onClick = {
                                tagEditorSelections = if (selected) {
                                    tagEditorSelections - tag.second
                                } else {
                                    tagEditorSelections + tag.second
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HailDialogLabel(
                    text = stringResource(R.string.tag_working_action),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.tag_working_action_hint),
                    color = dialogColors.secondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            HailDialogOption(
                text = actionEntries.first(),
                selected = tagEditorAction == 0,
                showCheck = true,
                onClick = { tagEditorAction = 0 },
                modifier = Modifier.fillMaxWidth(),
            )
            if (actionEntries.size > 1) {
                Spacer(Modifier.height(8.dp))
                actionEntries.indices.drop(1).chunked(2).forEach { rowIndices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowIndices.forEach { index ->
                            HailDialogOption(
                                text = actionEntries[index],
                                selected = index == tagEditorAction,
                                showCheck = true,
                                onClick = { tagEditorAction = index },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowIndices.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            val editableTagId = tagEditorTagId
            val editableTagIndex = HailData.tags.indexOfFirst { it.second == editableTagId }
            if (!tagEditorNew &&
                targets == null &&
                editableTagId != null &&
                editableTagId != 0 &&
                editableTagIndex >= 0
            ) {
                Spacer(Modifier.height(4.dp))
                HailDialogTextButton(
                    text = stringResource(R.string.action_tag_remove),
                    destructive = true,
                    onClick = {
                        val removedTagId = editableTagId
                        val removeIndex = HailData.tags.indexOfFirst {
                            it.second == removedTagId
                        }
                        HailData.checkedList.toList().forEach { appInfo ->
                            if (appInfo.tagIdList.remove(removedTagId) &&
                                appInfo.tagIdList.isEmpty()
                            ) {
                                removeFromHome(appInfo, save = false)
                            }
                        }
                        if (removeIndex >= 0) HailData.tags.removeAt(removeIndex)
                        HailData.setTagWorkingAction(removedTagId, null)
                        HailData.saveApps()
                        HailData.saveTags()
                        tagVersion++
                        selectedTagIndex = (removeIndex - 1).coerceAtLeast(0)
                        closeTagEditor()
                        refresh()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(20.dp))
            HailDialogButtonRow(
                cancelText = stringResource(android.R.string.cancel),
                confirmText = stringResource(android.R.string.ok),
                onCancel = ::closeTagEditor,
                onConfirm = {
                    val tagName = tagEditorName.text.trim()
                    val selectedAction = actionValues.getOrNull(tagEditorAction)
                    val saveTagId = tagEditorTagId
                    val saveTagIndex = HailData.tags.indexOfFirst {
                        it.second == saveTagId
                    }
                    var shouldClose = true
                    when {
                        tagEditorNew -> {
                            val tagId = tagName.hashCode()
                            when {
                                tagName.isEmpty() -> {
                                    tagEditorError =
                                        resources.getString(R.string.tag_name_required)
                                    shouldClose = false
                                }

                                hasTagNameConflict(HailData.tags, tagName, tagId) -> {
                                    tagEditorError = resources.getString(R.string.tag_name_exists)
                                    shouldClose = false
                                }

                                else -> {
                                    HailData.tags.add(tagName to tagId)
                                    HailData.setTagWorkingAction(tagId, selectedAction)
                                    HailData.saveTags()
                                    tagVersion++
                                }
                            }
                        }

                        targets != null -> {
                            var newTagId: Int? = null
                            val tagId = tagName.hashCode()
                            if (tagName.isNotEmpty() &&
                                hasTagNameConflict(HailData.tags, tagName, tagId)
                            ) {
                                tagEditorError = resources.getString(R.string.tag_name_exists)
                                shouldClose = false
                            } else {
                                if (tagName.isNotEmpty()) {
                                    HailData.tags.add(tagName to tagId)
                                    HailData.setTagWorkingAction(tagId, selectedAction)
                                    HailData.saveTags()
                                    newTagId = tagId
                                }
                                val finalTags = tagEditorSelections + listOfNotNull(newTagId)
                                val targetPackages = targets
                                    .mapTo(mutableSetOf()) { it.packageName }
                                HailData.checkedList
                                    .filter { it.packageName in targetPackages }
                                    .forEach { info ->
                                        info.tagIdList.clear()
                                        info.tagIdList.addAll(finalTags)
                                        if (info.tagIdList.isEmpty()) {
                                            removeFromHome(info, save = false)
                                        }
                                    }
                                HailData.saveApps()
                                tagVersion++
                                refresh()
                            }
                        }

                        saveTagId != null && saveTagIndex >= 0 -> {
                            if (tagName.isEmpty()) {
                                tagEditorError = resources.getString(R.string.tag_name_required)
                                shouldClose = false
                            } else {
                                val oldTag = HailData.tags[saveTagIndex]
                                val defaultTab = saveTagId == 0
                                val nameChanged = tagName != oldTag.first
                                val newId = when {
                                    defaultTab -> 0
                                    nameChanged -> tagName.hashCode()
                                    else -> oldTag.second
                                }
                                if (hasTagNameConflict(
                                        HailData.tags,
                                        tagName,
                                        newId,
                                        excludedTagId = oldTag.second,
                                    )
                                ) {
                                    tagEditorError = resources.getString(R.string.tag_name_exists)
                                    shouldClose = false
                                } else {
                                    HailData.tags[saveTagIndex] = tagName to newId
                                    if (!defaultTab && newId != oldTag.second) {
                                        HailData.checkedList.forEach {
                                            val index = it.tagIdList.indexOf(oldTag.second)
                                            if (index >= 0) it.tagIdList[index] = newId
                                        }
                                        HailData.saveApps()
                                    }
                                    HailData.replaceTagWorkingAction(
                                        oldTag.second,
                                        newId,
                                        selectedAction,
                                    )
                                    HailData.saveTags()
                                    tagVersion++
                                    selectedTagIndex = HailData.tags
                                        .indexOfFirst { it.second == newId }
                                        .coerceAtLeast(0)
                                    refresh()
                                }
                            }
                        }
                    }
                    if (shouldClose) closeTagEditor()
                },
            )
        }
    }

    message?.let {
        WindowDialog(
            show = true,
            title = it.title,
            summary = it.message,
            onDismissRequest = { message = null },
        ) {
            Button(
                onClick = { message = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

@Composable
private fun TagButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    HailSelectablePill(
        text = text,
        selected = selected,
        onClick = onClick,
        height = 32.dp,
        maxWidth = 160.dp,
        onLongClick = onLongClick,
    )
}

@Composable
private fun AddTagButton(onClick: () -> Unit) {
    val hapticClick = rememberHapticClick(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val darkTheme = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val pureColor = if (darkTheme) Color.Black else Color.White
    val inverseColor = if (darkTheme) Color.White else Color.Black
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .background(pureColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = hapticClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.action_tag_add),
            tint = inverseColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FabBubbleAction(
    text: String,
    onClick: () -> Unit,
) {
    val hapticClick = rememberHapticClick(onClick)
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .height(42.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = hapticClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppGridTopMask() {
    // Clip scrolling icons at the first row's icon edge, not at the tag row.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppGridItemTopPadding)
            .background(MiuixTheme.colorScheme.surface),
    )
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    selected: List<AppInfo>,
    multiselect: Boolean,
    workingMode: String,
    appLabels: Map<String, String>,
    refreshToken: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
) {
    if (apps.isEmpty()) {
        EmptyState(
            text = stringResource(R.string.nothing_here),
            modifier = modifier,
        )
        return
    }
    val columns = HailData.homeIconCount
    val iconSize = when (columns) {
        3 -> 88.dp
        4 -> 78.dp
        5 -> 66.dp
        6 -> 58.dp
        7 -> 50.dp
        else -> 44.dp
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(if (columns <= 4) 6.dp else 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
            items(apps, key = { it.packageName }) { info ->
                val hapticClick = rememberHapticClick { onClick(info) }
                val interactionSource = remember { MutableInteractionSource() }
                val isSelected = info in selected
                val state = remember(info.packageName, workingMode, refreshToken) {
                    info.state(workingMode)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) {
                                MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = hapticClick,
                            onLongClick = { onLongClick(info) },
                        )
                        .padding(
                            horizontal = 2.dp,
                            vertical = AppGridItemTopPadding,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        AppIcon(
                            appInfo = info,
                            size = iconSize,
                            showContainer = false,
                            contentPadding = 0.dp,
                        )
                        if (state == AppInfo.State.FROZEN) {
                            Image(
                                painter = painterResource(R.drawable.hail_frozen_ice),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(iconSize * 0.60f)
                                    .alpha(0.92f),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                    if (!HailData.textlessMode) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = appLabels[info.packageName] ?: info.name.toString(),
                            color = when {
                                isSelected -> MiuixTheme.colorScheme.primary
                                state == AppInfo.State.NOT_FOUND -> MiuixTheme.colorScheme.error
                                else -> MiuixTheme.colorScheme.onSurface
                            },
                            fontSize = HailData.homeFontSize.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
}

private fun String.workingActionTitleId(): Int = when (this) {
    HailData.STOP -> R.string.working_action_stop
    HailData.DISABLE -> R.string.working_action_disable
    HailData.HIDE -> R.string.working_action_hide
    HailData.SUSPEND -> R.string.working_action_suspend
    else -> R.string.working_action_unavailable
}

private fun String.operationMessageId(frozen: Boolean): Int =
    when (HailData.workingAction(this)) {
        HailData.STOP -> if (frozen) R.string.msg_force_stopped else R.string.msg_restored
        HailData.DISABLE -> if (frozen) R.string.msg_disabled else R.string.msg_enabled
        HailData.HIDE -> if (frozen) R.string.msg_hidden else R.string.msg_unhidden
        HailData.SUSPEND -> if (frozen) R.string.msg_suspended else R.string.msg_unsuspended
        else -> if (frozen) R.string.msg_freeze else R.string.msg_unfreeze
    }

private fun String.operationStatusId(frozen: Boolean): Int =
    when (HailData.workingAction(this)) {
        HailData.STOP -> if (frozen) R.string.status_force_stopped else R.string.status_restored
        HailData.DISABLE -> if (frozen) R.string.status_disabled else R.string.status_enabled
        HailData.HIDE -> if (frozen) R.string.status_hidden else R.string.status_unhidden
        HailData.SUSPEND -> if (frozen) R.string.status_suspended else R.string.status_unsuspended
        else -> if (frozen) R.string.action_freeze else R.string.action_unfreeze
    }

private fun tagDisplayName(tagName: String): String {
    val suffix = app.getString(R.string.tag)
    return if (tagName.endsWith(suffix, ignoreCase = true)) tagName
    else app.getString(R.string.tag_name_format, tagName)
}
