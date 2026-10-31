@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aistra.hail.ui.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.state.ToggleableState
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.extensions.exportFileName
import com.aistra.hail.ui.common.AppIcon
import com.aistra.hail.ui.common.EmptyState
import com.aistra.hail.ui.common.HailAction
import com.aistra.hail.ui.common.HailActionGrid
import com.aistra.hail.ui.common.HailDialog
import com.aistra.hail.ui.common.HailDialogAddChip
import com.aistra.hail.ui.common.HailDialogButtonRow
import com.aistra.hail.ui.common.HailDialogChip
import com.aistra.hail.ui.common.HailDialogLabel
import com.aistra.hail.ui.common.HailDialogTextField
import com.aistra.hail.ui.common.HailDialogWideButton
import com.aistra.hail.ui.common.HailScreenScaffold
import com.aistra.hail.ui.common.rememberHailDialogColors
import com.aistra.hail.ui.common.rememberHapticClick
import com.aistra.hail.ui.home.hasTagNameConflict
import com.aistra.hail.ui.main.HailRoute
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.PinyinSearch
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

private data class AppsMessage(
    val title: String? = null,
    val message: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

@Composable
fun AppsScreen(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var settingsVersion by remember { mutableIntStateOf(0) }
    var reloadVersion by remember { mutableIntStateOf(0) }
    var showFilters by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf(TextFieldValue()) }
    var newTagError by remember { mutableStateOf<String?>(null) }
    var actionApp by remember { mutableStateOf<ApplicationInfo?>(null) }
    var message by remember { mutableStateOf<AppsMessage?>(null) }
    var pendingUninstall by remember { mutableStateOf<ApplicationInfo?>(null) }
    var exportPackage by remember { mutableStateOf<String?>(null) }
    val originalChecked = remember {
        HailData.checkedList.mapTo(mutableSetOf()) { it.packageName }
    }
    var draftChecked by remember { mutableStateOf<Set<String>>(originalChecked) }
    var tags by remember { mutableStateOf(HailData.tags.toList()) }
    var targetTagId by remember {
        mutableIntStateOf(tags.firstOrNull()?.second ?: 0)
    }

    val exportApk = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"),
    ) { uri ->
        val packageName = exportPackage
        exportPackage = null
        if (uri == null || packageName == null) return@rememberLauncherForActivityResult
        scope.launch {
            val info = HPackages.getApplicationInfoOrNull(packageName) ?: return@launch
            runCatching {
                withContext(Dispatchers.IO) {
                    FileInputStream(info.sourceDir).use { source ->
                        context.contentResolver.openOutputStream(uri, "rwt").use { target ->
                            if (target != null) HFiles.copy(source, target)
                        }
                    }
                }
            }.onSuccess {
                HUI.showToast(R.string.msg_extract_apk, uri.toString())
            }.onFailure {
                HUI.showToast(
                    R.string.operation_failed,
                    it.localizedMessage ?: "Unknown",
                    true,
                )
            }
        }
    }

    LaunchedEffect(reloadVersion) {
        loading = true
        apps = withContext(Dispatchers.IO) { HPackages.getInstalledApplications() }
        loading = false
    }

    fun refresh() {
        reloadVersion++
        settingsVersion++
    }

    val displayApps = remember(apps, query, settingsVersion) {
        val pm = app.packageManager
        apps.filter {
            val system = it.flags and ApplicationInfo.FLAG_SYSTEM != 0
            ((HailData.filterUserApps && !system) ||
                (HailData.filterSystemApps && system)) &&
                ((HailData.filterFrozenApps && AppManager.isAppFrozen(it.packageName)) ||
                    (HailData.filterUnfrozenApps && !AppManager.isAppFrozen(it.packageName))) &&
                (
                    FuzzySearch.search(it.packageName, query) ||
                        FuzzySearch.search(it.loadLabel(pm).toString(), query) ||
                        PinyinSearch.searchPinyinAll(it.loadLabel(pm).toString(), query)
                    )
        }.let { filtered ->
            when (HailData.sortBy) {
                HailData.SORT_INSTALL -> filtered.sortedBy {
                    HPackages.getUnhiddenPackageInfoOrNull(it.packageName)?.firstInstallTime ?: 0
                }

                HailData.SORT_UPDATE -> filtered.sortedByDescending {
                    HPackages.getUnhiddenPackageInfoOrNull(it.packageName)?.lastUpdateTime ?: 0
                }

                else -> filtered.sortedWith(NameComparator)
            }
        }
    }
    val addedPackages = draftChecked - originalChecked
    val removedPackages = originalChecked - draftChecked
    val hasPendingChanges = addedPackages.isNotEmpty() || removedPackages.isNotEmpty()
    val clearQueryClick = rememberHapticClick { query = "" }
    val filterClick = rememberHapticClick { showFilters = true }

    HailScreenScaffold(
        title = stringResource(R.string.action_add_operation_apps),
        route = route,
        onNavigate = onNavigate,
        onBack = onBack,
        showUnavailableQuickAction = true,
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = clearQueryClick) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }
            IconButton(onClick = filterClick) {
                Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.filter_by))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.msg_selected,
                        draftChecked.size.toString(),
                    ),
                    style = MiuixTheme.textStyles.title4,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { showGroupDialog = true },
                    enabled = hasPendingChanges,
                    minWidth = 56.dp,
                    minHeight = 36.dp,
                    insideMargin = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_next),
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
            SearchBar(
                inputField = {
                    InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        label = stringResource(R.string.action_search),
                    )
                },
                expanded = false,
                onExpandedChange = {},
            ) {}
            if (loading) {
                EmptyState(text = stringResource(R.string.app_name))
            } else if (displayApps.isEmpty()) {
                EmptyState(text = stringResource(R.string.nothing_here))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayApps, key = { it.packageName }) { info ->
                        val checked = info.packageName in draftChecked
                        val hapticToggle = rememberHapticClick {
                            draftChecked = if (checked) {
                                draftChecked - info.packageName
                            } else {
                                draftChecked + info.packageName
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            insideMargin = PaddingValues(14.dp),
                            onClick = hapticToggle,
                            onLongPress = { actionApp = info },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(info = info, size = 44.dp)
                                Spacer(Modifier.size(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = info.loadLabel(app.packageManager).toString(),
                                        style = MiuixTheme.textStyles.body1,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = info.packageName,
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Checkbox(
                                    state = if (checked) ToggleableState.On else ToggleableState.Off,
                                    onClick = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        val sortEntries = listOf(
            stringResource(R.string.sort_name),
            stringResource(R.string.sort_install),
            stringResource(R.string.sort_update),
        )
        val selectedSortIndex = when (HailData.sortBy) {
            HailData.SORT_INSTALL -> 1
            HailData.SORT_UPDATE -> 2
            else -> 0
        }
        HailDialog(
            title = stringResource(R.string.title_filter_sort),
            onDismissRequest = { showFilters = false },
        ) {
            HailDialogLabel(stringResource(R.string.sort_by))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sortEntries.forEachIndexed { index, entry ->
                    HailDialogChip(
                        text = entry,
                        selected = index == selectedSortIndex,
                        onClick = {
                            HailData.changeAppsSort(
                                when (index) {
                                    1 -> HailData.SORT_INSTALL
                                    2 -> HailData.SORT_UPDATE
                                    else -> HailData.SORT_NAME
                                },
                            )
                            settingsVersion++
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            HailDialogLabel(stringResource(R.string.filter_scope))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HailDialogChip(
                    text = stringResource(R.string.filter_user_apps),
                    selected = HailData.filterUserApps,
                    onClick = {
                        val enabled = !HailData.filterUserApps
                        HailData.changeAppsFilter(HailData.FILTER_USER_APPS, enabled)
                        if (enabled) {
                            HailData.changeAppsFilter(
                                HailData.FILTER_SYSTEM_APPS,
                                false,
                            )
                        }
                        settingsVersion++
                    },
                    modifier = Modifier.weight(1f),
                )
                HailDialogChip(
                    text = stringResource(R.string.filter_system_apps),
                    selected = HailData.filterSystemApps,
                    onClick = {
                        if (!HailData.filterSystemApps) {
                            message = AppsMessage(
                                message = resources.getString(R.string.freeze_system_app),
                                actionLabel = resources.getString(R.string.action_continue),
                                action = {
                                    HailData.changeAppsFilter(
                                        HailData.FILTER_SYSTEM_APPS,
                                        true,
                                    )
                                    HailData.changeAppsFilter(
                                        HailData.FILTER_USER_APPS,
                                        false,
                                    )
                                    settingsVersion++
                                },
                            )
                        } else {
                            HailData.changeAppsFilter(
                                HailData.FILTER_SYSTEM_APPS,
                                false,
                            )
                            settingsVersion++
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HailDialogChip(
                    text = stringResource(R.string.filter_frozen_apps),
                    selected = HailData.filterFrozenApps,
                    onClick = {
                        HailData.changeAppsFilter(
                            HailData.FILTER_FROZEN_APPS,
                            !HailData.filterFrozenApps,
                        )
                        settingsVersion++
                    },
                    modifier = Modifier.weight(1f),
                )
                HailDialogChip(
                    text = stringResource(R.string.filter_unfrozen_apps),
                    selected = HailData.filterUnfrozenApps,
                    onClick = {
                        HailData.changeAppsFilter(
                            HailData.FILTER_UNFROZEN_APPS,
                            !HailData.filterUnfrozenApps,
                        )
                        settingsVersion++
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(20.dp))
            HailDialogWideButton(
                text = stringResource(R.string.action_done),
                onClick = { showFilters = false },
            )
        }
    }

    if (showGroupDialog && hasPendingChanges) {
        val addedCount = addedPackages.size
        val removedCount = removedPackages.size
        val dialogColors = rememberHailDialogColors()
        val summary = when {
            addedCount > 0 && removedCount > 0 -> resources.getString(
                R.string.msg_add_and_remove_apps,
                addedCount,
                removedCount,
            )
            addedCount > 0 -> resources.getString(R.string.msg_add_apps_to_group, addedCount)
            else -> resources.getString(R.string.msg_remove_apps_from_home, removedCount)
        }
        HailDialog(
            title = stringResource(R.string.title_add_to_group),
            showHandle = false,
            titleAlign = TextAlign.Start,
            onDismissRequest = { showGroupDialog = false },
        ) {
            Text(
                text = summary,
                color = dialogColors.secondary,
                fontSize = 14.sp,
            )
            if (addedCount > 0) {
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        HailDialogChip(
                            text = tag.first,
                            selected = targetTagId == tag.second,
                            leadingCheck = true,
                            onClick = { targetTagId = tag.second },
                        )
                    }
                    HailDialogAddChip(
                        text = stringResource(R.string.tag_create_new),
                        onClick = {
                            newTagName = TextFieldValue()
                            newTagError = null
                            showNewTagDialog = true
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            HailDialogButtonRow(
                cancelText = stringResource(android.R.string.cancel),
                confirmText = stringResource(R.string.action_confirm_add),
                onCancel = { showGroupDialog = false },
                onConfirm = {
                    removedPackages.forEach {
                        HailData.removeCheckedApp(it, saveApps = false)
                    }
                    addedPackages.forEach {
                        HailData.addCheckedApp(
                            packageName = it,
                            tagId = targetTagId,
                            saveApps = false,
                        )
                    }
                    HailData.saveApps()
                    showNewTagDialog = false
                    showGroupDialog = false
                    onBack()
                },
            )
        }
    }

    if (showNewTagDialog) {
        val dialogColors = rememberHailDialogColors()
        HailDialog(
            title = stringResource(R.string.action_tag_add),
            onDismissRequest = { showNewTagDialog = false },
        ) {
            HailDialogLabel(stringResource(R.string.tag_name_label))
            Spacer(Modifier.height(8.dp))
            HailDialogTextField(
                value = newTagName,
                onValueChange = {
                    newTagName = it
                    newTagError = null
                },
                placeholder = stringResource(R.string.tag_name_placeholder),
            )
            newTagError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = dialogColors.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(20.dp))
            HailDialogButtonRow(
                cancelText = stringResource(android.R.string.cancel),
                confirmText = stringResource(android.R.string.ok),
                onCancel = { showNewTagDialog = false },
                onConfirm = {
                    val name = newTagName.text.trim()
                    val tagId = name.hashCode()
                    when {
                        name.isEmpty() -> {
                            newTagError = resources.getString(R.string.tag_name_required)
                        }

                        hasTagNameConflict(HailData.tags, name, tagId) -> {
                            newTagError = resources.getString(R.string.tag_name_exists)
                        }

                        else -> {
                            HailData.tags.add(name to tagId)
                            HailData.saveTags()
                            tags = HailData.tags.toList()
                            targetTagId = tagId
                            showNewTagDialog = false
                        }
                    }
                },
            )
        }
    }

    actionApp?.let { info ->
        WindowBottomSheet(
            show = true,
            title = info.loadLabel(app.packageManager).toString(),
            onDismissRequest = { actionApp = null },
        ) {
            val actions = listOf(
                HailAction(
                    title = stringResource(R.string.action_details),
                    icon = Icons.Outlined.Info,
                ) {
                    actionApp = null
                    HUI.startActivity(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        HPackages.packageUri(info.packageName),
                    )
                },
                HailAction(
                    title = stringResource(R.string.action_export_clipboard),
                    icon = Icons.Outlined.ContentCopy,
                ) {
                    actionApp = null
                    HUI.copyText(info.packageName)
                    HUI.showToast(R.string.msg_text_copied, info.packageName)
                },
                HailAction(
                    title = stringResource(R.string.action_extract_apk),
                    icon = Icons.Outlined.FileDownload,
                ) {
                    actionApp = null
                    exportPackage = info.packageName
                    exportApk.launch(
                        HPackages.getUnhiddenPackageInfoOrNull(info.packageName)?.exportFileName
                            ?: info.packageName,
                    )
                },
                HailAction(
                    title = stringResource(R.string.action_reinstall),
                    icon = Icons.Outlined.Refresh,
                ) {
                    actionApp = null
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            AppManager.reinstallApp(info.packageName)
                        }
                        if (success) refresh()
                        else HUI.showToast(R.string.operation_failed, info.packageName)
                    }
                },
                HailAction(
                    title = stringResource(R.string.action_uninstall),
                    icon = Icons.Outlined.DeleteOutline,
                    destructive = true,
                ) {
                    actionApp = null
                    when {
                        HPackages.isAppUninstalled(info.packageName) ->
                            HUI.showToast(R.string.app_not_installed)

                        info.packageName == app.packageName -> when {
                            HPolicy.isDeviceOwnerActive -> {
                                message = AppsMessage(
                                    title = resources.getString(R.string.title_remove_owner),
                                    message = resources.getString(R.string.msg_remove_owner),
                                    actionLabel = resources.getString(R.string.action_continue),
                                    action = {
                                        HPolicy.setOrganizationName()
                                        HPolicy.removeDeviceOwner()
                                    },
                                )
                            }

                            HPolicy.isProfileOwner -> HPolicy.removeProfileOwner()
                            HPolicy.isAdminActive -> HPolicy.removeActiveAdmin()
                            else -> pendingUninstall = info
                        }

                        HailData.workingMode == HailData.MODE_DEFAULT -> {
                            AppManager.uninstallApp(info.packageName)
                            refresh()
                        }

                        else -> pendingUninstall = info
                    }
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

    pendingUninstall?.let { info ->
        WindowDialog(
            show = true,
            title = info.loadLabel(app.packageManager).toString(),
            summary = stringResource(R.string.msg_uninstall),
            onDismissRequest = { pendingUninstall = null },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { pendingUninstall = null },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        pendingUninstall = null
                        scope.launch {
                            val success = withContext(Dispatchers.IO) {
                                AppManager.uninstallApp(info.packageName)
                            }
                            if (success) refresh()
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }

    message?.let { dialog ->
        WindowDialog(
            show = true,
            title = dialog.title,
            summary = dialog.message,
            onDismissRequest = { message = null },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { message = null },
                    modifier = Modifier.weight(1f),
                )
                if (dialog.actionLabel != null && dialog.action != null) {
                    Button(
                        onClick = {
                            dialog.action.invoke()
                            message = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(dialog.actionLabel)
                    }
                }
            }
        }
    }
}
