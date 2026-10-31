package com.aistra.hail.xposed

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.aistra.hail.BuildConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE, application = Application::class)
class XposedSettingsSyncTest {
    private val context = RedirectedPreferencesContext(RuntimeEnvironment.getApplication())
    private val name = "${BuildConfig.APPLICATION_ID}_preferences"
    private val destination get() = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    @Test
    fun oldSettingsSurviveSwitchingToTheClassicPreferenceDirectory() {
        val original = writeLegacy("""
            <map>
                <string name="working_mode">su_disable</string>
                <boolean name="mipush_enhance" value="false"/>
                <int name="home_icon_count" value="6"/>
                <float name="home_font_size_f" value="17.5"/>
                <long name="timestamp" value="12345678901"/>
                <set name="tags"><string>work</string><string>games</string></set>
            </map>
        """.trimIndent())
        val before = original.readText()
        XposedSettingsSync.prepare(context)
        assertEquals("su_disable", destination.getString("working_mode", null))
        assertFalse(destination.getBoolean("mipush_enhance", true))
        assertEquals(6, destination.getInt("home_icon_count", 0))
        assertEquals(17.5f, destination.getFloat("home_font_size_f", 0f), 0f)
        assertEquals(12345678901L, destination.getLong("timestamp", 0))
        assertEquals(setOf("work", "games"), destination.getStringSet("tags", null))
        assertEquals(before, original.readText())
    }

    @Test
    fun migrationDoesNotOverwriteSettingsAlreadyInTheNewDirectory() {
        destination.edit().putString("working_mode", "shizuku_disable").commit()
        writeLegacy("<map><string name=\"working_mode\">su_disable</string></map>")
        XposedSettingsSync.prepare(context)
        assertEquals("shizuku_disable", destination.getString("working_mode", null))
    }

    @Test
    fun malformedLegacySettingsAreNotPartiallyImported() {
        val original = writeLegacy("<map><string name=\"working_mode\">su_disable</string><boolean")
        XposedSettingsSync.prepare(context)
        assertTrue(destination.all.isEmpty())
        assertTrue(original.isFile)
    }

    @Test
    fun updatingThePushSwitchDoesNotWriteASeparatePreferenceFile() {
        XposedSettingsSync.prepare(context)
        destination.edit().putString("working_mode", "su_disable").commit()
        XposedSettingsSync.update(false)
        assertEquals("su_disable", destination.getString("working_mode", null))
        assertTrue(pushPreferences().all.isEmpty())
    }

    @Test
    fun togglingThePushSwitchBroadcastsTheNewValue() {
        XposedSettingsSync.prepare(context)
        XposedSettingsSync.update(false)

        val intents = shadowOf(RuntimeEnvironment.getApplication())
            .broadcastIntents
            .filter { it.action == MiPushPreferences.ACTION_SETTINGS_CHANGED }
        assertEquals(setOf("android", "com.xiaomi.xmsf"), intents.map { it.`package` }.toSet())
        intents.forEach {
            assertFalse(it.getBooleanExtra(MiPushPreferences.KEY_ENABLED, true))
        }
    }

    @Test
    fun togglingTheOnDemandSwitchBroadcastsToSystemServer() {
        XposedSettingsSync.prepare(context)
        XposedSettingsSync.updateOnDemandUnfreeze(true)

        val intents = shadowOf(RuntimeEnvironment.getApplication())
            .broadcastIntents
            .filter { it.action == OnDemandUnfreezeSettings.ACTION_SETTINGS_CHANGED }
        assertEquals(listOf("android"), intents.map { it.`package` })
        intents.forEach {
            assertTrue(it.getBooleanExtra(OnDemandUnfreezeSettings.KEY_ENABLED, false))
        }
    }

    private fun pushPreferences() = context.getSharedPreferences("mipush", Context.MODE_PRIVATE)

    private fun writeLegacy(xml: String): File =
        File(context.applicationInfo.dataDir, "shared_prefs/$name.xml").apply {
            parentFile!!.mkdirs()
            writeText(xml)
        }

    private class RedirectedPreferencesContext(base: Context) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences("lsposed_$name", mode)
    }
}
