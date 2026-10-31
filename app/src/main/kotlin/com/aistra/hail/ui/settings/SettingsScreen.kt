package com.aistra.hail.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Adb
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.CalendarDisguise
import com.aistra.hail.app.HailBackupManager
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.common.HailGroup
import com.aistra.hail.ui.common.HailScreenScaffold
import com.aistra.hail.ui.common.rememberBooleanPreference
import com.aistra.hail.ui.common.rememberFloatPreference
import com.aistra.hail.ui.common.rememberHapticClick
import com.aistra.hail.ui.common.rememberIntPreference
import com.aistra.hail.ui.common.rememberStringPreference
import com.aistra.hail.ui.main.HailRoute
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HShell
import com.aistra.hail.utils.HTarget
import com.aistra.hail.utils.HUI
import com.aistra.hail.xposed.XposedSettingsSync
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private data class MessageDialog(
    val title: String? = null,
    val message: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

@Composable
fun SettingsScreen(
    route: HailRoute,
    onNavigate: (HailRoute) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val workingMode = rememberStringPreference(HailData.WORKING_MODE, HailData.MODE_DEFAULT)
    val biometric = rememberBooleanPreference(
        HailData.BIOMETRIC_LOGIN,
        HailData.DEFAULT_BIOMETRIC_LOGIN,
    )
    val calendarDisguise = rememberBooleanPreference(
        HailData.CALENDAR_DISGUISE,
        HailData.DEFAULT_CALENDAR_DISGUISE,
    )
    val appTheme = rememberStringPreference(HailData.APP_THEME, HailData.FOLLOW_SYSTEM)
    val homeIconCount = rememberIntPreference(HailData.HOME_ICON_COUNT, 5)
    val homeFontSize = rememberFloatPreference(HailData.HOME_FONT_SIZE, 14f)
    val textlessMode = rememberBooleanPreference(
        HailData.TEXTLESS_MODE,
        HailData.DEFAULT_TEXTLESS_MODE,
    )
    val hapticFeedback = rememberBooleanPreference(
        HailData.HAPTIC_FEEDBACK,
        HailData.DEFAULT_HAPTIC_FEEDBACK,
    )
    val floatingNavigationBar = rememberBooleanPreference(
        HailData.FLOATING_NAVIGATION_BAR,
        HailData.DEFAULT_FLOATING_NAVIGATION_BAR,
    )
    val actionButtonPosition = rememberStringPreference(
        HailData.ACTION_BUTTON_POSITION,
        HailData.DEFAULT_ACTION_BUTTON_POSITION,
    )
    val fuzzySearch = rememberBooleanPreference(
        HailData.FUZZY_SEARCH,
        HailData.DEFAULT_FUZZY_SEARCH,
    )
    val mipushEnhance = rememberBooleanPreference(
        HailData.MIPUSH_ENHANCE,
        HailData.DEFAULT_MIPUSH_ENHANCE,
    )
    val onDemandUnfreeze = rememberBooleanPreference(
        HailData.ON_DEMAND_UNFREEZE,
        HailData.DEFAULT_ON_DEMAND_UNFREEZE,
    )

    var messageDialog by remember { mutableStateOf<MessageDialog?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    var terminalCommand by remember { mutableStateOf(TextFieldValue()) }
    var pendingIslandMode by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = HailBackupManager.createBackup()
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                        it.write(json)
                    } ?: error("Cannot open backup file")
                }
            }.onSuccess {
                HUI.showToast(R.string.backup_success)
            }.onFailure {
                HUI.showToast(R.string.backup_failed, true)
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        it.readText()
                    } ?: error("Cannot open backup file")
                }
            }.onSuccess { json ->
                runCatching { HailBackupManager.summarize(json) }
                    .onSuccess {
                        messageDialog = MessageDialog(
                            title = resources.getString(R.string.action_restore),
                            message = resources.getString(R.string.restore_confirm_message),
                            actionLabel = resources.getString(R.string.action_restore),
                            action = {
                                runCatching { HailBackupManager.restore(json) }
                                    .onSuccess {
                                        HUI.showToast(R.string.restore_success)
                                        (context as? Activity)?.recreate()
                                    }
                                    .onFailure {
                                        HUI.showToast(R.string.restore_failed, true)
                                    }
                            },
                        )
                    }
                    .onFailure {
                        HUI.showToast(R.string.restore_failed, true)
                    }
            }.onFailure {
                HUI.showToast(R.string.restore_failed, true)
            }
        }
    }

    val islandPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val mode = pendingIslandMode
        pendingIslandMode = null
        if (granted && mode != null) {
            HIsland.checkOwnerApp()
            workingMode.second(mode)
        }
    }

    fun commitWorkingMode(mode: String) {
        if (hasUnrestoredAppOperations()) {
            messageDialog = MessageDialog(
                message = resources.getString(R.string.restore_apps_before_switching_mode),
            )
            return
        }
        workingMode.second(mode)
    }

    fun canUseWorkingMode(mode: String): Boolean {
        when {
            mode.startsWith(HailData.OWNER) -> {
                if (!HPolicy.isDeviceOwnerActive) {
                    messageDialog = MessageDialog(
                        title = resources.getString(R.string.title_set_owner),
                        message = resources.getString(R.string.msg_set_owner, HPolicy.ADB_COMMAND),
                        actionLabel = resources.getString(android.R.string.copy),
                        action = { HUI.copyText(HPolicy.ADB_COMMAND) },
                    )
                    return false
                }
            }

            mode.startsWith(HailData.DHIZUKU) -> {
                runCatching {
                    Dhizuku.init(app)
                    if (Dhizuku.isPermissionGranted()) return true
                    scope.launch {
                        val granted = callbackFlow {
                            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                                override fun onRequestPermission(grantResult: Int) {
                                    trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                                }
                            })
                            awaitClose()
                        }.first()
                        if (granted) {
                            commitWorkingMode(mode)
                            if (HTarget.O) HDhizuku.setDelegatedScopes()
                        }
                    }
                }.onFailure {
                    HLog.e(it)
                    HUI.showToast(R.string.permission_denied)
                }
                return false
            }

            mode.startsWith(HailData.SU) -> {
                if (!HShell.checkSU) {
                    HUI.showToast(R.string.permission_denied)
                    return false
                }
            }

            mode.startsWith(HailData.SHIZUKU) -> {
                runCatching {
                    when {
                        Shizuku.isPreV11() -> error("Unsupported Shizuku version")
                        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> return true
                        Shizuku.shouldShowRequestPermissionRationale() -> {
                            HUI.showToast(R.string.permission_denied)
                            return false
                        }

                        else -> scope.launch {
                            val granted = callbackFlow {
                                val requestCode = 0
                                val listener = Shizuku.OnRequestPermissionResultListener {
                                        code, result ->
                                    if (code == requestCode) {
                                        trySendBlocking(result == PackageManager.PERMISSION_GRANTED)
                                    }
                                }
                                Shizuku.addRequestPermissionResultListener(listener)
                                Shizuku.requestPermission(requestCode)
                                awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
                            }.first()
                            if (granted) commitWorkingMode(mode)
                        }
                    }
                }.onFailure {
                    HLog.e(it)
                    HUI.showToast(R.string.shizuku_missing)
                }
                return false
            }

            mode.startsWith(HailData.ISLAND) -> {
                val alreadyGranted = when (mode) {
                    HailData.MODE_ISLAND_HIDE -> HIsland.freezePermissionGranted()
                    else -> HIsland.suspendPermissionGranted()
                }
                if (!alreadyGranted) {
                    pendingIslandMode = mode
                    islandPermissionLauncher.launch(
                        if (mode == HailData.MODE_ISLAND_HIDE) {
                            HIsland.PERMISSION_FREEZE_PACKAGE
                        } else {
                            HIsland.PERMISSION_SUSPEND_PACKAGE
                        },
                    )
                    return false
                }
                HIsland.checkOwnerApp()
            }

            mode.startsWith(HailData.PRIVAPP) -> {
                if (!HPackages.isPrivilegedApp(app.packageName)) {
                    HUI.showToast(R.string.permission_denied)
                    return false
                }
            }
        }
        return true
    }

    fun requestWorkingMode(mode: String) {
        if (mode == workingMode.first.value || !canUseWorkingMode(mode)) return
        commitWorkingMode(mode)
    }

    val permissionEntries = stringArrayResource(R.array.working_permission_entries)
    val themeEntries = stringArrayResource(R.array.app_theme_entries)
    val actionButtonPositionEntries = listOf(
        stringResource(R.string.action_button_position_bottom_bar),
        stringResource(R.string.action_button_position_bottom_right),
    )
    val permission = HailData.workingPermission(workingMode.first.value)
    val supportedActions = HailData.supportedWorkingActions(permission)
    val actionValues = supportedActions.ifEmpty { listOf(HailData.MODE_DEFAULT) }
    val actionEntries = if (supportedActions.isEmpty()) {
        listOf(stringResource(R.string.working_action_unavailable))
    } else {
        supportedActions.map { resources.getString(it.workingActionTitleId()) }
    }
    val hasUnrestoredOperations = remember { hasUnrestoredAppOperations() }
    val showRestoreFirst = {
        messageDialog = MessageDialog(
            message = resources.getString(R.string.restore_apps_before_switching_mode),
        )
    }
    val selectedActionIndex = actionValues
        .indexOf(HailData.workingAction(workingMode.first.value)).coerceAtLeast(0)
    val removeOwnerClick = rememberHapticClick {
        messageDialog = MessageDialog(
            title = resources.getString(R.string.title_remove_owner),
            message = resources.getString(R.string.msg_remove_owner),
            actionLabel = resources.getString(R.string.action_continue),
            action = {
                HPolicy.setOrganizationName()
                HPolicy.removeDeviceOwner()
            },
        )
    }

    HailScreenScaffold(
        title = stringResource(R.string.title_settings),
        route = route,
        onNavigate = onNavigate,
        subtitle = HailData.VERSION,
        showUnavailableQuickAction = true,
        actions = {
            if (HPolicy.isDeviceOwnerActive) {
                IconButton(
                    onClick = removeOwnerClick,
                ) {
                    Icon(
                        Icons.Outlined.AdminPanelSettings,
                        contentDescription = stringResource(R.string.title_remove_owner),
                    )
                }
            }
        },
    ) { innerPadding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HailGroup(title = stringResource(R.string.working_mode)) {
                    Box {
                        OverlayDropdownPreference(
                            items = permissionEntries.toList(),
                            selectedIndex = HailData.WORKING_PERMISSION_VALUES
                                .indexOf(permission).coerceAtLeast(0),
                            title = stringResource(R.string.working_permission),
                            startAction = { PreferenceIcon(Icons.Outlined.Adb) },
                            enabled = !hasUnrestoredOperations,
                            onSelectedIndexChange = { index ->
                                requestWorkingMode(
                                    HailData.combineWorkingMode(
                                        HailData.WORKING_PERMISSION_VALUES[index],
                                        HailData.workingAction(workingMode.first.value),
                                    ),
                                )
                            },
                        )
                        if (hasUnrestoredOperations) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                        indication = null,
                                        onClick = showRestoreFirst,
                                    ),
                            )
                        }
                    }
                    Box {
                        OverlayDropdownPreference(
                            items = actionEntries,
                            selectedIndex = selectedActionIndex,
                            title = stringResource(R.string.default_working_mode),
                            enabled = !hasUnrestoredOperations &&
                                supportedActions.isNotEmpty(),
                            startAction = { PreferenceIcon(Icons.Outlined.Tune) },
                            onSelectedIndexChange = { index ->
                                requestWorkingMode(
                                    HailData.combineWorkingMode(permission, actionValues[index]),
                                )
                            },
                        )
                        if (hasUnrestoredOperations) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                        indication = null,
                                        onClick = showRestoreFirst,
                                    ),
                            )
                        }
                    }
                    ArrowPreference(
                        title = stringResource(R.string.action_add_operation_apps),
                        startAction = { PreferenceIcon(Icons.Outlined.AddBox) },
                        onClick = { onNavigate(HailRoute.Apps) },
                    )
                }
            }

            item {
                HailGroup(title = stringResource(R.string.title_security)) {
                    SwitchPreference(
                        checked = biometric.first.value,
                        onCheckedChange = biometric.second,
                        title = stringResource(R.string.action_biometric),
                        startAction = { PreferenceIcon(Icons.Outlined.Fingerprint) },
                    )
                    SwitchPreference(
                        checked = calendarDisguise.first.value,
                        onCheckedChange = { enabled ->
                            calendarDisguise.second(enabled)
                            CalendarDisguise.apply(enabled)
                        },
                        title = stringResource(R.string.calendar_disguise),
                        startAction = { PreferenceIcon(Icons.Outlined.CalendarMonth) },
                    )
                }
            }

            item {
                HailGroup(title = stringResource(R.string.title_appearance)) {
                    OverlayDropdownPreference(
                        items = themeEntries.toList(),
                        selectedIndex = HailData.APP_THEME_VALUES
                            .indexOf(appTheme.first.value).coerceAtLeast(0),
                        title = stringResource(R.string.app_theme),
                        startAction = { PreferenceIcon(Icons.Outlined.DarkMode) },
                        onSelectedIndexChange = { index ->
                            val value = HailData.APP_THEME_VALUES[index]
                            appTheme.second(value)
                            app.setAppTheme(value)
                        },
                    )
                    SwitchPreference(
                        checked = floatingNavigationBar.first.value,
                        onCheckedChange = floatingNavigationBar.second,
                        title = stringResource(R.string.floating_navigation_bar),
                        startAction = { PreferenceIcon(Icons.Outlined.ViewAgenda) },
                    )
                    OverlayDropdownPreference(
                        items = actionButtonPositionEntries,
                        selectedIndex = HailData.ACTION_BUTTON_POSITION_VALUES
                            .indexOf(actionButtonPosition.first.value).coerceAtLeast(0),
                        title = stringResource(R.string.action_button_position),
                        startAction = { PreferenceIcon(Icons.Outlined.SwapHoriz) },
                        onSelectedIndexChange = { index ->
                            actionButtonPosition.second(
                                HailData.ACTION_BUTTON_POSITION_VALUES[index],
                            )
                        },
                    )
                    SliderPreference(
                        value = homeIconCount.first.value.toFloat(),
                        onValueChange = {
                            homeIconCount.second(it.roundToInt().coerceIn(3, 8))
                        },
                        title = stringResource(R.string.home_icon_size),
                        valueText = homeIconCount.first.value.toString(),
                        valueRange = 3f..8f,
                        steps = 4,
                        showKeyPoints = true,
                        keyPoints = listOf(3f, 4f, 5f, 6f, 7f, 8f),
                        startAction = { PreferenceIcon(Icons.Outlined.Apps) },
                    )
                    SliderPreference(
                        value = homeFontSize.first.value,
                        onValueChange = homeFontSize.second,
                        title = stringResource(R.string.home_font_size),
                        valueText = "%.0f".format(homeFontSize.first.value),
                        valueRange = 11f..16f,
                        steps = 4,
                        showKeyPoints = true,
                        keyPoints = listOf(11f, 12f, 13f, 14f, 15f, 16f),
                        startAction = { PreferenceIcon(Icons.Outlined.TextFields) },
                    )
                    SwitchPreference(
                        checked = textlessMode.first.value,
                        onCheckedChange = textlessMode.second,
                        title = stringResource(R.string.textless_mode),
                        startAction = { PreferenceIcon(Icons.Outlined.VisibilityOff) },
                    )
                    SwitchPreference(
                        checked = hapticFeedback.first.value,
                        onCheckedChange = hapticFeedback.second,
                        title = stringResource(R.string.haptic_feedback),
                        startAction = { PreferenceIcon(Icons.Outlined.Vibration) },
                    )
                }
            }

            item {
                HailGroup(title = stringResource(R.string.title_customize)) {
                    SwitchPreference(
                        checked = mipushEnhance.first.value,
                        onCheckedChange = {
                            mipushEnhance.second(it)
                            XposedSettingsSync.update(it)
                        },
                        title = stringResource(R.string.mipush_enhance),
                        startAction = { PreferenceIcon(Icons.Outlined.NotificationsActive) },
                    )
                    SwitchPreference(
                        checked = onDemandUnfreeze.first.value,
                        onCheckedChange = {
                            onDemandUnfreeze.second(it)
                            XposedSettingsSync.updateOnDemandUnfreeze(it)
                        },
                        title = stringResource(R.string.on_demand_unfreeze),
                        startAction = { PreferenceIcon(Icons.Outlined.Link) },
                    )
                    SwitchPreference(
                        checked = fuzzySearch.first.value,
                        onCheckedChange = fuzzySearch.second,
                        title = stringResource(R.string.fuzzy_search),
                        startAction = { PreferenceIcon(Icons.Outlined.Android) },
                    )
                    if (HailData.workingMode.startsWith(HailData.SU) ||
                        HailData.workingMode.startsWith(HailData.SHIZUKU)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.action_terminal),
                            startAction = { PreferenceIcon(Icons.Outlined.Terminal) },
                            onClick = { showTerminal = true },
                        )
                    }
                }
            }

            item {
                HailGroup(title = stringResource(R.string.title_backup_restore)) {
                    ArrowPreference(
                        title = stringResource(R.string.action_backup),
                        startAction = { PreferenceIcon(Icons.Outlined.Backup) },
                        onClick = {
                            val timestamp = SimpleDateFormat(
                                "yyyyMMdd-HHmmss",
                                Locale.US,
                            ).format(Date())
                            backupLauncher.launch("hail-backup-$timestamp.json")
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.action_restore),
                        startAction = { PreferenceIcon(Icons.Outlined.Restore) },
                        onClick = {
                            restoreLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/plain",
                                    "application/octet-stream",
                                ),
                            )
                        },
                    )
                }
            }

            item {
                HailGroup(title = stringResource(R.string.title_other)) {
                    ArrowPreference(
                        title = stringResource(R.string.title_about),
                        startAction = { PreferenceIcon(Icons.Outlined.Info) },
                        onClick = { onNavigate(HailRoute.About) },
                    )
                }
            }
        }
    }

    messageDialog?.let { dialog ->
        WindowDialog(
            show = true,
            title = dialog.title,
            onDismissRequest = { messageDialog = null },
        ) {
            SelectionContainer {
                Text(
                    text = dialog.message,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { messageDialog = null },
                    modifier = Modifier.weight(1f),
                )
                if (dialog.actionLabel != null && dialog.action != null) {
                    Button(
                        onClick = {
                            dialog.action.invoke()
                            messageDialog = null
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(dialog.actionLabel)
                    }
                } else if (dialog.title == null) {
                    Button(
                        onClick = { messageDialog = null },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }

    if (showTerminal) {
        WindowDialog(
            show = true,
            title = stringResource(R.string.action_terminal),
            onDismissRequest = { showTerminal = false },
        ) {
            TextField(
                value = terminalCommand,
                onValueChange = { terminalCommand = it },
                label = stringResource(R.string.command),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { showTerminal = false },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val command = terminalCommand.text
                        showTerminal = false
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                AppManager.execute(command)
                            }
                            if (result.first != 0 || !result.second.isNullOrBlank()) {
                                messageDialog = MessageDialog(
                                    title = if (result.first != 0) {
                                        resources.getString(R.string.operation_failed, result.first.toString())
                                    } else {
                                        resources.getString(R.string.action_terminal)
                                    },
                                    message = result.second.orEmpty(),
                                    actionLabel = resources.getString(android.R.string.copy).takeIf {
                                        !result.second.isNullOrBlank()
                                    },
                                    action = result.second?.let { output ->
                                        { HUI.copyText(output) }
                                    },
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun PreferenceIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
    )
}

private fun hasUnrestoredAppOperations(): Boolean =
    HailData.appOperationsSnapshot().any { (workingMode, packageName) ->
        AppManager.isAppFrozen(packageName, workingMode)
    }

private fun String.workingActionTitleId(): Int = when (this) {
    HailData.STOP -> R.string.working_action_stop
    HailData.DISABLE -> R.string.working_action_disable
    HailData.HIDE -> R.string.working_action_hide
    HailData.SUSPEND -> R.string.working_action_suspend
    else -> R.string.working_action_unavailable
}
