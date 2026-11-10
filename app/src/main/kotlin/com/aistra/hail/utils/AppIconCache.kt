package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache
import me.zhanghai.android.appiconloader.AppIconLoader
import java.io.File
import java.io.FileOutputStream

/**
 * @author Rikka
 * Source
 * https://raw.githubusercontent.com/RikkaApps/Shizuku/master/manager/src/main/java/moe/shizuku/manager/utils/AppIconCache.kt
 */
object AppIconCache {

    private const val PERSISTED_ICON_SIZE = 256
    private const val ICON_DIR = "app_icons"

    private class AppIconLruCache constructor(maxSize: Int) :
        LruCache<Triple<String, Int, Int>, Bitmap>(maxSize) {

        override fun sizeOf(key: Triple<String, Int, Int>, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val lruCache: LruCache<Triple<String, Int, Int>, Bitmap>

    private val appIconLoaders = mutableMapOf<Int, AppIconLoader>()

    init {
        // Initialize app icon lru cache
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()
        lruCache = AppIconLruCache(availableCacheSize)
    }

    private fun get(packageName: String, userId: Int, size: Int): Bitmap? {
        return lruCache[Triple(packageName, userId, size)]
    }

    fun getCachedBitmap(packageName: String, userId: Int, size: Int): Bitmap? =
        get(packageName, userId, size)

    private fun put(packageName: String, userId: Int, size: Int, bitmap: Bitmap) {
        if (get(packageName, userId, size) == null) {
            lruCache.put(Triple(packageName, userId, size), bitmap)
        }
    }

    @SuppressLint("NewApi")
    fun getOrLoadBitmap(context: Context, info: ApplicationInfo, userId: Int, size: Int): Bitmap {
        val cachedBitmap = get(info.packageName, userId, size)
        if (cachedBitmap != null) {
            return cachedBitmap
        }
        var loader = appIconLoaders[size]
        if (loader == null) {
            loader = AppIconLoader(size, false, context)
            appIconLoaders[size] = loader
        }
        val bitmap = loader.loadIcon(info, false)
        put(info.packageName, userId, size, bitmap)
        return bitmap
    }

    private fun iconDir(context: Context) = File(context.filesDir, ICON_DIR)

    private fun iconFile(context: Context, packageName: String, userId: Int) =
        File(iconDir(context), "$packageName@$userId.png")

    /**
     * Saves the original icon to disk so it can still be shown after the app
     * is uninstalled. An existing file is kept as is.
     */
    fun persistIcon(context: Context, packageName: String, userId: Int): Boolean {
        val file = iconFile(context, packageName, userId)
        if (file.exists()) return true
        val info = HPackages.getApplicationInfoOrNull(packageName) ?: return false
        val bitmap = runCatching {
            getOrLoadBitmap(context, info, userId, PERSISTED_ICON_SIZE)
        }.getOrNull() ?: return false
        return runCatching {
            iconDir(context).mkdirs()
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        }.getOrDefault(false)
    }

    fun getPersistedBitmap(
        context: Context,
        packageName: String,
        userId: Int,
        size: Int,
    ): Bitmap? {
        get(packageName, userId, size)?.let { return it }
        val file = iconFile(context, packageName, userId)
        if (!file.exists()) return null
        val bitmap = runCatching {
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull() ?: return null
        val scaled = if (bitmap.width == size && bitmap.height == size) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, size, size, true)
        }
        put(packageName, userId, size, scaled)
        return scaled
    }

    fun deletePersistedIcon(context: Context, packageName: String, userId: Int) {
        runCatching { iconFile(context, packageName, userId).delete() }
    }

    /** Drops persisted icons of apps that are no longer on the home list. */
    fun prunePersistedIcons(context: Context, keepPackages: Set<String>, userId: Int) {
        val suffix = "@$userId.png"
        iconDir(context).listFiles()?.forEach { file ->
            val name = file.name
            if (name.endsWith(suffix) && name.removeSuffix(suffix) !in keepPackages) {
                runCatching { file.delete() }
            }
        }
    }
}
