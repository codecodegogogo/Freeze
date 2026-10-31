package com.aistra.hail.xposed

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Binder
import com.aistra.hail.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Lets frozen (disabled) apps take part in link resolution again and unfreezes
 * the target right before it is launched.
 *
 * Disabled components are filtered out by PackageManagerService unless the
 * caller passes MATCH_DISABLED_COMPONENTS, which browsers never do. The first
 * hook adds that flag for ACTION_VIEW intents with a data URI, so a browser
 * sees the installed (but frozen) handler instead of "no app found". The
 * second hook enables the target package in ActivityStarter before the system
 * starts its process.
 */
class OnDemandUnfreezeHook : IXposedHookLoadPackage {
    @Volatile
    private var classLoader: ClassLoader? = null

    @Volatile
    private var packageManager: Any? = null

    /**
     * The launch check queries candidates itself; its results must keep the
     * real enabled state so frozen handlers are still detected as frozen.
     */
    private val internalQuery = ThreadLocal<Boolean>()

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (param.packageName != "android") return
        classLoader = param.classLoader
        OnDemandUnfreezeSettings.init(param.classLoader)
        hookIntentResolution(param.classLoader)
        hookActivityStart(param.classLoader)
        hookSettingsBroadcast(param.classLoader)
    }

    private fun hookIntentResolution(classLoader: ClassLoader) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (internalQuery.get() == true) return
                if (!OnDemandUnfreezeSettings.isEnabled()) return
                val intent = param.args.getOrNull(0) as? Intent ?: return
                if (intent.action != Intent.ACTION_VIEW || intent.data == null) return
                val updated = withDisabledComponents(param.args.getOrNull(2)) ?: return
                param.args[2] = updated
                param.setObjectExtra(RESOLUTION_PATCH_KEY, true)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if (internalQuery.get() == true) return
                if (param.getObjectExtra(RESOLUTION_PATCH_KEY) != true) return
                runCatching { markDisabledResultsEnabled(param.result) }
                    .onFailure { log("patch resolution result failed: $it") }
            }
        }
        listOf(
            "com.android.server.pm.PackageManagerService",
            "com.android.server.pm.ComputerEngine",
        ).forEach { className ->
            runCatching {
                val clazz = XposedHelpers.findClass(className, classLoader)
                XposedBridge.hookAllMethods(clazz, "queryIntentActivitiesInternal", hook)
                log("intent resolution hook installed on $className")
            }.onFailure { log("intent resolution hook failed on $className: $it") }
        }
    }

    /**
     * A disabled activity is reported with enabled=false even after the flag is
     * added. Copy the info and report it as enabled so browsers that check the
     * flag still offer the frozen app; the package is enabled before launch.
     */
    private fun markDisabledResultsEnabled(result: Any?) {
        val list = result as? List<*> ?: return
        list.forEach { item ->
            val resolveInfo = item as? ResolveInfo ?: return@forEach
            val activityInfo = resolveInfo.activityInfo ?: return@forEach
            if (activityInfo.enabled) return@forEach
            val copy = ActivityInfo(activityInfo)
            copy.enabled = true
            activityInfo.applicationInfo?.let {
                copy.applicationInfo = ApplicationInfo(it).apply { enabled = true }
            }
            resolveInfo.activityInfo = copy
        }
    }

    private fun hookActivityStart(classLoader: ClassLoader) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!OnDemandUnfreezeSettings.isEnabled()) return
                val request = findRequest(param) ?: return
                runCatching { unfreezeBeforeLaunch(request) }
                    .onFailure { log("unfreeze before launch failed: $it") }
            }
        }
        listOf(
            "com.android.server.wm.ActivityStarter",
            "com.android.server.am.ActivityStarter",
        ).forEach { className ->
            runCatching {
                val clazz = XposedHelpers.findClass(className, classLoader)
                XposedBridge.hookAllMethods(clazz, "executeRequest", hook)
                log("activity starter hook installed on $className")
            }.onFailure { log("activity starter hook failed on $className: $it") }
        }
    }

    /**
     * Android 15 changed executeRequest to take the Request as an argument;
     * older releases keep it in the mRequest field.
     */
    private fun findRequest(param: XC_MethodHook.MethodHookParam): Any? {
        param.args.forEach { arg ->
            if (arg != null && arg.javaClass.simpleName == "Request" &&
                arg.javaClass.name.contains("ActivityStarter")
            ) {
                return arg
            }
        }
        return runCatching { XposedHelpers.getObjectField(param.thisObject, "mRequest") }
            .getOrNull()
    }

    private fun hookSettingsBroadcast(classLoader: ClassLoader) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args.forEach { arg ->
                    if (arg is Intent) OnDemandUnfreezeSettings.updateFromBroadcast(arg)
                }
            }
        }
        runCatching {
            val ams = XposedHelpers.findClass(
                "com.android.server.am.ActivityManagerService",
                classLoader,
            )
            XposedBridge.hookAllMethods(ams, "broadcastIntentWithFeature", hook)
            XposedBridge.hookAllMethods(ams, "broadcastIntent", hook)
            log("settings broadcast hook installed")
        }.onFailure { log("settings broadcast hook failed: $it") }
    }

    private fun unfreezeBeforeLaunch(request: Any) {
        val intent = XposedHelpers.getObjectField(request, "intent") as? Intent ?: return
        val userId = runCatching { XposedHelpers.getIntField(request, "userId") }.getOrDefault(0)
        val activityInfo = XposedHelpers.getObjectField(request, "activityInfo") as? ActivityInfo
        val packageName = activityInfo?.packageName ?: resolveTargetPackage(intent, userId) ?: return
        if (packageName == "android" || packageName == BuildConfig.APPLICATION_ID) return
        if (!unfreezeIfNeeded(packageName, userId)) return
        // ActivityStarter already resolved the activity before executeRequest,
        // so update the cached info instead of clearing it (clearing makes the
        // start fail on Android 15).
        if (activityInfo != null && !activityInfo.enabled) {
            markRequestEnabled(request, activityInfo)
        }
    }

    private fun markRequestEnabled(request: Any, activityInfo: ActivityInfo) {
        val copy = ActivityInfo(activityInfo)
        copy.enabled = true
        activityInfo.applicationInfo?.let {
            copy.applicationInfo = ApplicationInfo(it).apply { enabled = true }
        }
        XposedHelpers.setObjectField(request, "activityInfo", copy)
        val resolveInfo = runCatching {
            XposedHelpers.getObjectField(request, "resolveInfo")
        }.getOrNull() as? ResolveInfo
        if (resolveInfo?.activityInfo?.packageName == copy.packageName) {
            resolveInfo.activityInfo = copy
        }
    }

    private fun resolveTargetPackage(intent: Intent, userId: Int): String? {
        intent.component?.packageName?.let { return it }
        intent.`package`?.let { return it }
        if (intent.data == null) return null
        val candidates = queryIntentActivities(intent, userId)
        if (candidates.isEmpty()) return null
        // With a startable handler present the system uses it (or shows the
        // chooser), so only a link whose handlers are all frozen is unfrozen.
        if (candidates.any { isStartable(it.activityInfo) }) return null
        return candidates.first().activityInfo?.packageName
    }

    private fun queryIntentActivities(intent: Intent, userId: Int): List<ResolveInfo> {
        val pm = packageManagerService() ?: return emptyList()
        val method = pm.javaClass.methods.firstOrNull { method ->
            method.name == "queryIntentActivities" &&
                method.parameterTypes.size == 4 &&
                method.parameterTypes[0] == Intent::class.java
        } ?: return emptyList()
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS
        val flagsArg = if (method.parameterTypes[2] == Int::class.javaPrimitiveType) {
            flags
        } else {
            flags.toLong()
        }
        val identity = Binder.clearCallingIdentity()
        internalQuery.set(true)
        return try {
            @Suppress("UNCHECKED_CAST")
            method.invoke(pm, intent, null, flagsArg, userId) as? List<ResolveInfo> ?: emptyList()
        } catch (t: Throwable) {
            log("query candidates failed: $t")
            emptyList()
        } finally {
            internalQuery.remove()
            Binder.restoreCallingIdentity(identity)
        }
    }

    private fun isStartable(activityInfo: ActivityInfo?): Boolean {
        if (activityInfo == null || !activityInfo.enabled) return false
        val appInfo = activityInfo.applicationInfo ?: return true
        if (!appInfo.enabled) return false
        if (appInfo.flags and ApplicationInfo.FLAG_SUSPENDED != 0) return false
        return !isHidden(appInfo)
    }

    private fun unfreezeIfNeeded(packageName: String, userId: Int): Boolean {
        val pm = packageManagerService() ?: return false
        val identity = Binder.clearCallingIdentity()
        return try {
            val appInfo = getApplicationInfo(pm, packageName, userId) ?: return false
            var changed = false
            if (isHidden(appInfo)) {
                changed = callBoolean(
                    pm,
                    "setApplicationHiddenSettingAsUser",
                    packageName,
                    false,
                    userId,
                ) || changed
            }
            if (appInfo.flags and ApplicationInfo.FLAG_SUSPENDED != 0) {
                changed = unsuspendPackage(pm, packageName, userId) || changed
            }
            // Only COMPONENT_ENABLED_STATE_DISABLED is set by Freeze; a state
            // chosen by the user in system settings is left untouched.
            if (getApplicationEnabledSetting(pm, packageName, userId) ==
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            ) {
                XposedHelpers.callMethod(
                    pm,
                    "setApplicationEnabledSetting",
                    packageName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                    userId,
                    BuildConfig.APPLICATION_ID,
                )
                XposedHelpers.callMethod(pm, "setPackageStoppedState", packageName, false, userId)
                changed = true
            }
            if (changed) log("unfroze $packageName userId=$userId")
            changed
        } catch (t: Throwable) {
            log("unfreeze $packageName failed: $t")
            false
        } finally {
            Binder.restoreCallingIdentity(identity)
        }
    }

    private fun getApplicationInfo(pm: Any, packageName: String, userId: Int): ApplicationInfo? {
        val method = pm.javaClass.methods.firstOrNull { method ->
            method.name == "getApplicationInfo" &&
                method.parameterTypes.size == 3 &&
                method.parameterTypes[0] == String::class.java
        } ?: return null
        val flags =
            PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS
        val flagsArg = if (method.parameterTypes[1] == Int::class.javaPrimitiveType) {
            flags
        } else {
            flags.toLong()
        }
        return method.invoke(pm, packageName, flagsArg, userId) as? ApplicationInfo
    }

    private fun getApplicationEnabledSetting(pm: Any, packageName: String, userId: Int): Int =
        runCatching {
            XposedHelpers.callMethod(pm, "getApplicationEnabledSetting", packageName, userId) as Int
        }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)

    private fun callBoolean(pm: Any, methodName: String, vararg args: Any?): Boolean =
        runCatching {
            XposedHelpers.callMethod(pm, methodName, *args) as Boolean
        }.getOrDefault(false)

    private fun unsuspendPackage(pm: Any, packageName: String, userId: Int): Boolean {
        val method = pm.javaClass.methods.firstOrNull {
            it.name == "setPackagesSuspendedAsUser"
        } ?: return false
        val args = method.parameterTypes.mapIndexed { index, type ->
            when {
                type.isArray && type.componentType == String::class.java -> arrayOf(packageName)
                type == java.lang.Boolean.TYPE -> false
                type == java.lang.Integer.TYPE ->
                    if (index == method.parameterTypes.lastIndex) userId else 0

                type == String::class.java -> BuildConfig.APPLICATION_ID
                else -> null
            }
        }.toTypedArray()
        method.invoke(pm, *args)
        return true
    }

    private fun isHidden(info: ApplicationInfo): Boolean = runCatching {
        val field = ApplicationInfo::class.java.getField("privateFlags")
        field.getInt(info) and PRIVATE_FLAG_HIDDEN != 0
    }.getOrDefault(false)

    private fun packageManagerService(): Any? {
        packageManager?.let { return it }
        val loader = classLoader ?: return null
        return runCatching {
            val activityThread = XposedHelpers.findClass("android.app.ActivityThread", loader)
            XposedHelpers.callStaticMethod(activityThread, "getPackageManager")
                .also { packageManager = it }
        }.getOrNull()
    }

    private fun withDisabledComponents(flags: Any?): Any? {
        if (flags is Int) {
            return if (flags and PackageManager.MATCH_DISABLED_COMPONENTS == 0) {
                flags or PackageManager.MATCH_DISABLED_COMPONENTS
            } else {
                null
            }
        }
        if (flags is Long) {
            val disabled = PackageManager.MATCH_DISABLED_COMPONENTS.toLong()
            return if (flags and disabled == 0L) flags or disabled else null
        }
        if (flags == null || flags.javaClass.name != RESOLVE_INFO_FLAGS_CLASS) return null
        val value = resolveInfoFlagsValue(flags) ?: return null
        if (value and PackageManager.MATCH_DISABLED_COMPONENTS.toLong() != 0L) return null
        return runCatching {
            XposedHelpers.callStaticMethod(
                flags.javaClass,
                "of",
                value or PackageManager.MATCH_DISABLED_COMPONENTS.toLong(),
            )
        }.getOrNull()
    }

    private fun resolveInfoFlagsValue(flags: Any): Long? =
        runCatching { XposedHelpers.callMethod(flags, "getValue") as Long }.getOrNull()
            ?: runCatching { XposedHelpers.getLongField(flags, "mValue") }.getOrNull()

    private fun log(message: String) {
        XposedBridge.log("Freeze-OnDemand: $message")
    }

    private companion object {
        private const val RESOLVE_INFO_FLAGS_CLASS = "android.content.pm.ResolveInfoFlags"
        private const val RESOLUTION_PATCH_KEY = "Freeze.OnDemand.patchResolution"
        private const val PRIVATE_FLAG_HIDDEN = 1
    }
}
