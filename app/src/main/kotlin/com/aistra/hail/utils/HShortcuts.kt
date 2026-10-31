package com.aistra.hail.utils

import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailApi
import com.aistra.hail.ui.api.ApiActivity

/**
 * Pinned launcher shortcuts for frozen apps. The shortcut starts [ApiActivity]
 * with [HailApi.ACTION_LAUNCH], so tapping it unfreezes and opens the app while
 * the app itself stays frozen until the shortcut is used.
 */
object HShortcuts {
    private const val LAUNCH_SHORTCUT_PREFIX = "launch_"

    fun addLaunchShortcut(info: AppInfo, label: CharSequence): Boolean {
        val applicationInfo = info.applicationInfo ?: return false
        val iconSize = (96 * app.resources.displayMetrics.density).toInt().coerceIn(96, 512)
        val bitmap = runCatching {
            AppIconCache.getOrLoadBitmap(app, applicationInfo, HPackages.myUserId, iconSize)
        }.getOrNull() ?: runCatching {
            app.packageManager.getApplicationIcon(applicationInfo).toBitmap(iconSize, iconSize)
        }.getOrNull() ?: return false
        val intent = launchShortcutIntent(info.packageName)
        val shortcut = ShortcutInfoCompat.Builder(
            app,
            LAUNCH_SHORTCUT_PREFIX + info.packageName,
        )
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithBitmap(bitmap))
            .setIntent(intent)
            .build()
        return runCatching {
            ShortcutManagerCompat.isRequestPinShortcutSupported(app) &&
                ShortcutManagerCompat.requestPinShortcut(app, shortcut, null)
        }.getOrDefault(false)
    }

    internal fun launchShortcutIntent(packageName: String): Intent =
        HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, packageName)
            .setClassName(app, ApiActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
