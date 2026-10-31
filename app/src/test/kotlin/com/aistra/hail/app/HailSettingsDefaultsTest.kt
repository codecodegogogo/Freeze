package com.aistra.hail.app

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp
import com.aistra.hail.HailApp.Companion.app
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HailApp::class)
class HailSettingsDefaultsTest {
    @Test
    fun settingsUseRequestedDefaultsWhenPreferencesAreEmpty() {
        PreferenceManager.getDefaultSharedPreferences(
            RuntimeEnvironment.getApplication(),
        ).edit().clear().commit()

        assertFalse(HailData.biometricLogin)
        assertFalse(HailData.calendarDisguise)
        assertFalse(HailData.floatingNavigationBar)
        assertEquals(HailData.ACTION_BUTTON_BOTTOM_RIGHT, HailData.actionButtonPosition)
        assertFalse(HailData.textlessMode)
        assertTrue(HailData.hapticFeedback)
        assertFalse(HailData.mipushEnhance)
        assertFalse(HailData.onDemandUnfreeze)
        assertFalse(HailData.fuzzySearch)
    }

    @Test
    fun disabledCalendarDisguiseUsesTheFreezeLauncher() {
        PreferenceManager.getDefaultSharedPreferences(
            RuntimeEnvironment.getApplication(),
        ).edit().clear().commit()

        CalendarDisguise.apply(HailData.calendarDisguise)

        val packageManager = app.packageManager
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            packageManager.getComponentEnabledSetting(
                ComponentName(app, CalendarDisguise.CALENDAR_LAUNCHER_NAME),
            ),
        )
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            packageManager.getComponentEnabledSetting(
                ComponentName(app, CalendarDisguise.HAIL_LAUNCHER_NAME),
            ),
        )
    }
}
