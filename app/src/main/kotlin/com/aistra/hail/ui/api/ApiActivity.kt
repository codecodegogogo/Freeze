package com.aistra.hail.ui.api

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.common.HailAction
import com.aistra.hail.ui.common.HailActionGrid
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.*
import androidx.lifecycle.lifecycleScope
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            if (handleAction(intent.action)) finish()
        }.onFailure(::setErrorDialog)
    }

    private fun handleAction(action: String?): Boolean {
        when (action) {
            Intent.ACTION_SHOW_APP_INFO -> {
                setContent { AppTheme { RedirectBottomSheet(requirePackage) } }
                return false
            }

            Intent.ACTION_VIEW -> return handleSchema(intent.data)

            HailApi.ACTION_LAUNCH -> {
                launchApp(requirePackage, runCatching { requireTagId }.getOrNull())
                return false
            }
            HailApi.ACTION_FREEZE -> setAppFrozen(requirePackage, true)
            HailApi.ACTION_UNFREEZE -> setAppFrozen(requirePackage, false)
            HailApi.ACTION_FREEZE_TAG -> {
                val tagId = requireTagId
                setListFrozen(
                    true,
                    HailData.checkedList.filter { tagId in it.tagIdList },
                    true,
                    HailData.tagWorkingMode(tagId),
                )
                return false
            }

            HailApi.ACTION_UNFREEZE_TAG -> {
                val tagId = requireTagId
                setListFrozen(
                    false,
                    HailData.checkedList.filter { tagId in it.tagIdList },
                    workingMode = HailData.tagWorkingMode(tagId),
                )
                return false
            }

            HailApi.ACTION_FREEZE_ALL -> {
                setListFrozen(true)
                return false
            }

            HailApi.ACTION_UNFREEZE_ALL -> {
                setListFrozen(false)
                return false
            }
            HailApi.ACTION_LOCK -> {
                lockScreen(false)
                return false
            }

            HailApi.ACTION_LOCK_FREEZE -> {
                lockScreen(true)
                return false
            }
            else -> throw IllegalArgumentException("Unknown action:\n$action")
        }
        return true
    }

    /**
     * Handle schema actions
     *
     * hail://launch?package=xxx
     * hail://freeze?package=xxx
     * hail://unfreeze?package=xxx
     * hail://freeze_tag?tag=xxx
     * hail://unfreeze_tag?tag=xxx
     * hail://freeze_all
     * hail://unfreeze_all
     * hail://lock
     * hail://lock_freeze
     */
    private fun handleSchema(uri: Uri?): Boolean {
        if (uri?.scheme != "hail") throw IllegalArgumentException("Unknown scheme:\n${uri?.scheme}")
        return handleAction(
            when (uri.host) {
                "launch" -> HailApi.ACTION_LAUNCH
                "freeze" -> HailApi.ACTION_FREEZE
                "unfreeze" -> HailApi.ACTION_UNFREEZE
                "freeze_tag" -> HailApi.ACTION_FREEZE_TAG
                "unfreeze_tag" -> HailApi.ACTION_UNFREEZE_TAG
                "freeze_all" -> HailApi.ACTION_FREEZE_ALL
                "unfreeze_all" -> HailApi.ACTION_UNFREEZE_ALL
                "lock" -> HailApi.ACTION_LOCK
                "lock_freeze" -> HailApi.ACTION_LOCK_FREEZE
                else -> throw IllegalArgumentException("Unknown host:\n${uri.host}")
            }
        )
    }

    private fun setErrorDialog(t: Throwable) = setContent { AppTheme { ErrorDialog(t) } }

    @Composable
    private fun RedirectBottomSheet(pkg: String) {
        WindowBottomSheet(
            show = true,
            title = HPackages.getApplicationInfoOrNull(pkg)?.loadLabel(packageManager)?.toString()
                ?: pkg,
            onDismissRequest = ::finish,
        ) {
            val actions = listOf(
                HailAction(
                    title = stringResource(R.string.action_launch),
                    icon = Icons.AutoMirrored.Outlined.Launch,
                ) {
                    runCatching {
                        launchApp(pkg)
                    }.onFailure(::setErrorDialog)
                },
                HailAction(
                    title = stringResource(R.string.action_freeze),
                    icon = Icons.Outlined.AcUnit,
                ) {
                    runCatching {
                        if (!HailData.isChecked(pkg)) HailData.addCheckedApp(pkg)
                        setAppFrozen(pkg, true)
                        finish()
                    }.onFailure(::setErrorDialog)
                },
                HailAction(
                    title = stringResource(R.string.action_unfreeze),
                    icon = Icons.Outlined.LockOpen,
                ) {
                    runCatching {
                        setAppFrozen(pkg, false)
                        finish()
                    }.onFailure(::setErrorDialog)
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

    @Composable
    private fun ErrorDialog(t: Throwable) {
        WindowDialog(
            show = true,
            summary = t.message ?: t.stackTraceToString(),
            onDismissRequest = ::finish,
        ) {
            Button(
                onClick = ::finish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }

    private val requirePackage: String
        get() = intent.run {
            if (action == Intent.ACTION_VIEW) data?.getQueryParameter(HailData.KEY_PACKAGE)
            else getStringExtra(
                if (action != Intent.ACTION_SHOW_APP_INFO) HailData.KEY_PACKAGE
                else if (HTarget.N) Intent.EXTRA_PACKAGE_NAME
                else "android.intent.extra.PACKAGE_NAME"
            )
        }?.also {
            HPackages.getApplicationInfoOrNull(it) ?: throw NameNotFoundException(getString(R.string.app_not_installed))
        } ?: throw IllegalArgumentException("Package must not be null")

    private val requireTagId: Int
        get() = intent.run {
            if (action == Intent.ACTION_VIEW) data?.getQueryParameter(HailData.KEY_TAG)
            else getStringExtra(HailData.KEY_TAG)
        }?.let {
            HailData.tags.find { tag -> tag.first == it }?.second
                ?: throw IllegalStateException("Tag unavailable:\n$it")
        } ?: throw IllegalArgumentException("Tag must not be null")

    private fun launchApp(pkg: String, tagId: Int? = null) {
        lifecycleScope.launch {
            runCatching {
                val workingMode = tagId?.let(HailData::tagWorkingMode)
                    ?: HailData.checkedList.find { it.packageName == pkg }
                        ?.let(HailData::appWorkingModeTag)
                        ?.second
                    ?: HailData.workingMode
                if (tagId != null) {
                    val tagApps = HailData.checkedList.filter { tagId in it.tagIdList }
                    executeListFrozen(
                        frozen = false,
                        list = tagApps,
                        workingMode = workingMode,
                    ) ?: throw IllegalStateException(getString(R.string.permission_denied))
                }
                withContext(Dispatchers.IO) {
                    if (AppManager.isAppFrozen(pkg, workingMode)) {
                        AppManager.setAppFrozen(pkg, false, workingMode)
                    }
                    if (workingMode == HailData.MODE_ISLAND_HIDE) {
                        HIsland.ensureLaunchIntentExists(pkg)
                    }
                }
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent == null) {
                    val stillFrozen = withContext(Dispatchers.IO) {
                        AppManager.isAppFrozen(pkg, workingMode)
                    }
                    if (stillFrozen) {
                        throw IllegalStateException(getString(R.string.permission_denied))
                    }
                    throw ActivityNotFoundException(getString(R.string.activity_not_found))
                }
                startActivity(launchIntent)
            }.onSuccess {
                finish()
            }.onFailure {
                setErrorDialog(it)
            }
        }
    }

    private fun setAppFrozen(pkg: String, frozen: Boolean) {
        if (frozen && !HailData.isChecked(pkg)) {
            throw SecurityException("Package not checked")
        }
        val workingMode = HailData.checkedList.find { it.packageName == pkg }
            ?.let(HailData::appWorkingModeTag)
            ?.second
            ?: HailData.workingMode
        if (AppManager.isAppFrozen(pkg, workingMode) != frozen &&
            !AppManager.setAppFrozen(pkg, frozen, workingMode)
        ) {
            throw IllegalStateException(getString(R.string.permission_denied))
        }
        HUI.showToast(
            if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
            HPackages.getApplicationInfoOrNull(pkg)?.loadLabel(packageManager) ?: pkg,
        )
    }

    private fun setListFrozen(
        frozen: Boolean,
        list: List<AppInfo> = HailData.checkedList,
        skipWhitelisted: Boolean = false,
        workingMode: String? = null,
    ) {
        val snapshot = list.toList()
        lifecycleScope.launch {
            runCatching {
                executeListFrozen(frozen, snapshot, skipWhitelisted, workingMode)
            }.onSuccess { result ->
                if (result == null) {
                    setErrorDialog(IllegalStateException(getString(R.string.permission_denied)))
                } else {
                    HUI.showToast(
                        if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                        result,
                    )
                    finish()
                }
            }.onFailure {
                setErrorDialog(it)
            }
        }
    }

    private suspend fun executeListFrozen(
        frozen: Boolean,
        list: List<AppInfo>,
        skipWhitelisted: Boolean = false,
        workingMode: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        if (workingMode != null) {
            applyListFrozen(frozen, list, skipWhitelisted, workingMode)
        } else {
            var applied = 0
            var failed = false
            list.groupBy(HailData::appWorkingModeTag).forEach { (tagMode, apps) ->
                val (_, mode) = tagMode
                if (mode == HailData.MODE_DEFAULT) {
                    failed = true
                    return@forEach
                }
                val result = applyListFrozen(frozen, apps, skipWhitelisted, mode)
                if (result == null) failed = true else applied += result.toIntOrNull() ?: 1
            }
            if (failed && applied == 0) null else applied.toString()
        }
    }

    private fun applyListFrozen(
        frozen: Boolean,
        list: List<AppInfo>,
        skipWhitelisted: Boolean,
        workingMode: String,
    ): String? {
        val filtered = list.filter {
            AppManager.isAppFrozen(it.packageName, workingMode) != frozen &&
                !(skipWhitelisted && it.whitelisted)
        }
        return AppManager.setListFrozen(frozen, workingMode, *filtered.toTypedArray())
    }

    private fun lockScreen(freezeAll: Boolean) {
        lifecycleScope.launch {
            runCatching {
                if (freezeAll) {
                    executeListFrozen(
                        frozen = true,
                        list = HailData.checkedList,
                    ) ?: throw IllegalStateException(getString(R.string.permission_denied))
                }
                if (!withContext(Dispatchers.IO) { AppManager.lockScreen }) {
                    throw IllegalStateException(getString(R.string.permission_denied))
                }
            }.onSuccess {
                finish()
            }.onFailure {
                setErrorDialog(it)
            }
        }
    }
}
