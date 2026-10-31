package com.aistra.hail.app

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.utils.HPackages
import com.aistra.hail.xposed.XposedSettingsSync
import org.json.JSONArray
import org.json.JSONObject

data class HailBackupSummary(
    val createdAt: Long,
    val settingsCount: Int,
    val appsCount: Int,
    val tagsCount: Int,
)

object HailBackupManager {
    private const val FORMAT = "hail-backup"
    private const val VERSION = 2

    fun createBackup(): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(app)
        val settings = JSONObject()
        preferences.all.forEach { (key, value) ->
            if (key != HailData.KEY_APP_OPERATIONS) {
                encodePreference(value)?.let { settings.put(key, it) }
            }
        }

        val apps = JSONArray().apply {
            HailData.checkedList.forEach { info ->
                put(
                    JSONObject()
                        .put("package", info.packageName)
                        .put("pinned", info.pinned)
                        .put("whitelisted", info.whitelisted)
                        .put("installed", !HPackages.isAppUninstalled(info.packageName))
                        .put("tags", JSONArray(info.tagIdList)),
                )
            }
        }
        val tags = JSONArray().apply {
            HailData.tags.forEach { (name, id) ->
                val action = HailData.tagWorkingAction(id)
                put(
                    JSONObject()
                        .put("name", name)
                        .put("id", id)
                        .put("workingMode", HailData.tagWorkingMode(id))
                        .put("action", action ?: JSONObject.NULL),
                )
            }
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAt", System.currentTimeMillis())
            .put("appVersion", HailData.VERSION)
            .put("settings", settings)
            .put("apps", apps)
            .put("tags", tags)
            .toString(2)
    }

    fun summarize(json: String): HailBackupSummary {
        val root = parse(json)
        return HailBackupSummary(
            createdAt = root.optLong("createdAt"),
            settingsCount = root.getJSONObject("settings").length(),
            appsCount = root.getJSONArray("apps").length(),
            tagsCount = root.getJSONArray("tags").length(),
        )
    }

    fun restore(json: String) {
        val root = parse(json)
        val preferences = PreferenceManager.getDefaultSharedPreferences(app)
        val restoredPreferences = mutableMapOf<String, Any>()
        root.getJSONObject("settings").keys().forEach { key ->
            decodePreference(root.getJSONObject("settings").getJSONObject(key))?.let {
                restoredPreferences[key] = it
            }
        }
        preferences.edit {
            clear()
            restoredPreferences.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                    else -> putString(key, value.toString())
                }
            }
        }

        val restoredApps = mutableListOf<AppInfo>()
        val apps = root.getJSONArray("apps")
        for (index in 0 until apps.length()) {
            val item = apps.getJSONObject(index)
            val tagIds = item.optJSONArray("tags") ?: JSONArray()
            restoredApps += AppInfo(
                packageName = item.getString("package"),
                pinned = item.optBoolean("pinned", false),
                whitelisted = item.optBoolean("whitelisted", false),
                tagIdList = MutableList(tagIds.length()) { tagIds.getInt(it) },
            )
        }
        HailData.checkedList.clear()
        HailData.checkedList.addAll(restoredApps)
        HailData.saveApps()

        val restoredTags = mutableListOf<Pair<String, Int>>()
        val restoredTagActions = mutableMapOf<Int, String>()
        val tags = root.getJSONArray("tags")
        for (index in 0 until tags.length()) {
            val item = tags.getJSONObject(index)
            val id = item.getInt("id")
            restoredTags += item.getString("name") to id
            val action = when {
                item.has("action") && !item.isNull("action") -> item.optString("action")
                !item.has("action") &&
                    item.has("workingMode") &&
                    !item.isNull("workingMode") -> item.optString("workingMode")

                else -> null
            }
            action?.takeIf { it.isNotBlank() }?.let {
                restoredTagActions[id] = it
            }
        }
        HailData.tags.clear()
        HailData.tags.addAll(restoredTags)
        HailData.saveTags()
        HailData.restoreTagWorkingActions(restoredTagActions)

        app.setAppTheme(HailData.appTheme)
        XposedSettingsSync.update(HailData.mipushEnhance)
        XposedSettingsSync.updateOnDemandUnfreeze(HailData.onDemandUnfreeze)
        CalendarDisguise.apply(HailData.calendarDisguise)
    }

    private fun parse(json: String): JSONObject {
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT) { "Unknown backup format" }
        require(root.getInt("version") in 1..VERSION) { "Unsupported backup version" }
        return root
    }

    private fun encodePreference(value: Any?): JSONObject? = when (value) {
        null -> null
        is Boolean -> JSONObject().put("type", "boolean").put("value", value)
        is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
        is Int -> JSONObject().put("type", "int").put("value", value)
        is Long -> JSONObject().put("type", "long").put("value", value)
        is String -> JSONObject().put("type", "string").put("value", value)
        is Set<*> -> JSONObject().put("type", "stringSet")
            .put("value", JSONArray(value.filterIsInstance<String>()))

        else -> null
    }

    private fun decodePreference(json: JSONObject): Any? = when (json.getString("type")) {
        "boolean" -> json.getBoolean("value")
        "float" -> json.getDouble("value").toFloat()
        "int" -> json.getInt("value")
        "long" -> json.getLong("value")
        "string" -> json.getString("value")
        "stringSet" -> json.getJSONArray("value").let { values ->
            buildSet {
                for (index in 0 until values.length()) add(values.getString(index))
            }
        }

        else -> null
    }

}
