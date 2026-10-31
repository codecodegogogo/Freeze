package com.aistra.hail.xposed

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.aistra.hail.app.HailData

/** Exposes the Xposed switches that system_server has to read. */
class XposedSettingsProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != OnDemandUnfreezeSettings.METHOD_IS_ENABLED) return null
        val enabled = PreferenceManager.getDefaultSharedPreferences(requireNotNull(context))
            .getBoolean(HailData.ON_DEMAND_UNFREEZE, HailData.DEFAULT_ON_DEMAND_UNFREEZE)
        return Bundle().apply {
            putBoolean(OnDemandUnfreezeSettings.KEY_ENABLED, enabled)
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
