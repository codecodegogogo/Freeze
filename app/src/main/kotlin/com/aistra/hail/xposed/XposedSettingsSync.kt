package com.aistra.hail.xposed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.util.Xml
import com.aistra.hail.BuildConfig
import org.xmlpull.v1.XmlPullParser
import java.io.File

object XposedSettingsSync {
    private lateinit var context: Context

    fun prepare(context: Context) {
        this.context = context.applicationContext
        // API 93 redirects preferences to LSPosed's directory. Preserve existing UI/mode settings.
        migrateLegacyPreferences(context)
    }

    fun start(defaultValue: Boolean) = update(defaultValue)

    fun update(value: Boolean) {
        listOf("android", "com.xiaomi.xmsf").forEach { packageName ->
            runCatching {
                context.sendBroadcast(
                    Intent(MiPushPreferences.ACTION_SETTINGS_CHANGED)
                        .setPackage(packageName)
                        .putExtra(MiPushPreferences.KEY_ENABLED, value),
                )
            }.onFailure { Log.w("Freeze-MiPush", "Cannot broadcast MiPush switch", it) }
        }
    }

    fun updateOnDemandUnfreeze(value: Boolean) {
        runCatching {
            context.sendBroadcast(
                Intent(OnDemandUnfreezeSettings.ACTION_SETTINGS_CHANGED)
                    .setPackage("android")
                    .putExtra(OnDemandUnfreezeSettings.KEY_ENABLED, value),
            )
        }.onFailure { Log.w("Freeze-OnDemand", "Cannot broadcast on-demand switch", it) }
    }

    internal fun migrateLegacyPreferences(context: Context) {
        val name = "${BuildConfig.APPLICATION_ID}_preferences"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        if (preferences.all.isNotEmpty()) return
        val file = File(context.applicationInfo.dataDir, "shared_prefs/$name.xml")
        val backup = File("${file.path}.bak")
        val source = if (backup.isFile) backup else file
        if (!source.isFile) return
        runCatching {
            val values = readLegacyPreferences(source)
            val editor = preferences.edit()
            values.forEach { (key, value) -> putValue(editor, key, value) }
            check(editor.commit()) { "Cannot persist migrated preferences" }
        }.onFailure { Log.w("Freeze-MiPush", "Preference migration failed; original file was kept", it) }
    }

    private fun readLegacyPreferences(file: File): Map<String, Any> = file.inputStream().use { input ->
        val parser = Xml.newPullParser().apply { setInput(input, "UTF-8") }
        val result = linkedMapOf<String, Any>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 2) continue
            val key = parser.getAttributeValue(null, "name") ?: continue
            val value = parser.getAttributeValue(null, "value")
            result[key] = when (parser.name) {
                "string" -> parser.nextText()
                "boolean" -> requireNotNull(value).toBooleanStrict()
                "int" -> requireNotNull(value).toInt()
                "long" -> requireNotNull(value).toLong()
                "float" -> requireNotNull(value).toFloat()
                "set" -> {
                    val strings = linkedSetOf<String>()
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.END_TAG && parser.depth == 2) break
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "string") {
                            strings += parser.nextText()
                        }
                    }
                    strings
                }
                else -> continue
            }
        }
        result
    }

    private fun putValue(editor: SharedPreferences.Editor, key: String, value: Any) {
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is String -> editor.putString(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
    }
}
