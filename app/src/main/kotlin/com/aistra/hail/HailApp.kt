package com.aistra.hail

import android.app.Application
import android.app.UiModeManager
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import com.aistra.hail.app.CalendarDisguise
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HTarget
import com.aistra.hail.xposed.XposedSettingsSync

class HailApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        XposedSettingsSync.prepare(this)
        CalendarDisguise.apply(HailData.calendarDisguise)
        XposedSettingsSync.start(HailData.mipushEnhance)
        XposedSettingsSync.updateOnDemandUnfreeze(HailData.onDemandUnfreeze)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            getSystemService<ShortcutManager>()?.removeAllDynamicShortcuts()
        }
        if (!HTarget.S) setAppTheme(HailData.appTheme)
        if (HailData.workingMode.startsWith(HailData.DHIZUKU)) HDhizuku.init()
    }

    fun setAppTheme(theme: String) {
        if (HTarget.S) getSystemService<UiModeManager>()!!.setApplicationNightMode(
            when (theme) {
                HailData.THEME_LIGHT -> UiModeManager.MODE_NIGHT_NO
                HailData.THEME_DARK -> UiModeManager.MODE_NIGHT_YES
                else -> UiModeManager.MODE_NIGHT_AUTO
            }
        )
        else AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                HailData.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                HailData.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }


    companion object {
        lateinit var app: HailApp private set
    }
}
