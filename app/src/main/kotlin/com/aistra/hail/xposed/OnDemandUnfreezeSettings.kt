package com.aistra.hail.xposed

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.aistra.hail.BuildConfig
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Keeps the "unfreeze on demand" switch available inside system_server.
 *
 * Hook callbacks only read [enabled]; the provider query runs on a background
 * thread, matching the MiPush switch so the system_server call path never waits
 * on cross-process I/O.
 */
internal object OnDemandUnfreezeSettings {
    const val KEY_ENABLED = "on_demand_unfreeze"
    const val ACTION_SETTINGS_CHANGED =
        BuildConfig.APPLICATION_ID + ".action.ON_DEMAND_UNFREEZE_CHANGED"
    const val PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".xposed"
    const val METHOD_IS_ENABLED = "is_on_demand_unfreeze_enabled"
    private const val RELOAD_INTERVAL_MS = 5000L

    @Volatile
    private var enabled = false

    @Volatile
    private var classLoader: ClassLoader? = null

    @Volatile
    private var systemContext: Context? = null

    @Volatile
    private var lastRefreshAttempt = -RELOAD_INTERVAL_MS

    @Volatile
    private var lastContextAttempt = -RELOAD_INTERVAL_MS

    @Volatile
    private var warned = false

    private val refreshInFlight = AtomicBoolean()
    private val stateVersion = AtomicLong()

    fun init(classLoader: ClassLoader) {
        this.classLoader = classLoader
        scheduleRefreshIfStale()
    }

    fun isEnabled(): Boolean {
        scheduleRefreshIfStale()
        return enabled
    }

    fun updateFromBroadcast(intent: Intent?) {
        if (intent == null ||
            intent.action != ACTION_SETTINGS_CHANGED ||
            !intent.hasExtra(KEY_ENABLED)
        ) {
            return
        }
        stateVersion.incrementAndGet()
        enabled = intent.getBooleanExtra(KEY_ENABLED, false)
        lastRefreshAttempt = SystemClock.uptimeMillis()
        XposedBridge.log("Freeze-OnDemand: switch changed to $enabled")
    }

    private fun scheduleRefreshIfStale() {
        val context = resolveSystemContext() ?: return
        val now = SystemClock.uptimeMillis()
        if (now - lastRefreshAttempt < RELOAD_INTERVAL_MS) return
        lastRefreshAttempt = now
        if (!refreshInFlight.compareAndSet(false, true)) return

        val version = stateVersion.get()
        Thread({
            try {
                val provided = readEnabledFromProvider(context)
                if (provided != null && stateVersion.get() == version) {
                    enabled = provided
                }
            } finally {
                refreshInFlight.set(false)
            }
        }, "Freeze-OnDemand-preference").apply { isDaemon = true }.start()
    }

    private fun resolveSystemContext(): Context? {
        systemContext?.let { return it }
        val loader = classLoader ?: return null
        val now = SystemClock.uptimeMillis()
        if (now - lastContextAttempt < RELOAD_INTERVAL_MS) return null
        lastContextAttempt = now
        return runCatching {
            val activityThread = XposedHelpers.findClass("android.app.ActivityThread", loader)
            val current = XposedHelpers.callStaticMethod(activityThread, "currentActivityThread")
                ?: return null
            val context = XposedHelpers.callMethod(current, "getSystemContext") as? Context
            context?.also { systemContext = it }
        }.getOrElse {
            warn("cannot resolve system context", it)
            null
        }
    }

    private fun readEnabledFromProvider(context: Context): Boolean? = runCatching {
        val result = context.contentResolver.call(
            Uri.parse("content://$PROVIDER_AUTHORITY"),
            METHOD_IS_ENABLED,
            null,
            null,
        )
        if (result == null || !result.containsKey(KEY_ENABLED)) null
        else result.getBoolean(KEY_ENABLED, false)
    }.getOrElse {
        warn("cannot query switch provider", it)
        null
    }

    private fun warn(message: String, error: Throwable) {
        if (warned) return
        warned = true
        XposedBridge.log("Freeze-OnDemand: $message: $error")
    }
}
