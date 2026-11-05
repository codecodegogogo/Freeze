package com.aistra.hail.xposed

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import de.robv.android.xposed.XposedHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    manifest = Config.NONE,
    shadows = [OnDemandUnfreezeHookTest.XposedHelpersShadow::class],
)
class OnDemandUnfreezeHookTest {
    private val hook = OnDemandUnfreezeHook()

    @Test
    fun `markRequestEnabled refreshes the application info used by recents`() {
        val stale = activityInfo(componentEnabled = true, appEnabled = false)
        val request = RequestStub(stale)
        val fresh = newApplicationInfo(enabled = true, suspended = true)

        call("markRequestEnabled", request, stale, fresh)

        val patched = request.activityInfo
        assertNotNull(patched)
        assertTrue(patched!!.enabled)
        assertNotNull(patched.applicationInfo)
        assertTrue(patched.applicationInfo.enabled)
        assertEquals(0, patched.applicationInfo.flags and ApplicationInfo.FLAG_SUSPENDED)
        assertSame(patched, request.resolveInfo?.activityInfo)
    }

    @Test
    fun `needs patch when only the application is disabled`() {
        assertTrue(
            call(
                "needsEnabledPatch",
                activityInfo(componentEnabled = true, appEnabled = false),
            ) as Boolean,
        )
        assertTrue(
            call(
                "needsEnabledPatch",
                activityInfo(componentEnabled = false, appEnabled = true),
            ) as Boolean,
        )
        assertTrue(
            call(
                "needsEnabledPatch",
                activityInfo(componentEnabled = true, appEnabled = true, suspended = true),
            ) as Boolean,
        )
        assertFalse(
            call(
                "needsEnabledPatch",
                activityInfo(componentEnabled = true, appEnabled = true),
            ) as Boolean,
        )
    }

    @Test
    fun `needs patch when the cached application info is missing`() {
        val info = ActivityInfo().apply {
            packageName = PACKAGE
            name = "$PACKAGE.MainActivity"
            enabled = true
            applicationInfo = null
        }
        assertTrue(call("needsEnabledPatch", info) as Boolean)
    }

    private fun activityInfo(
        componentEnabled: Boolean,
        appEnabled: Boolean,
        suspended: Boolean = false,
    ) = ActivityInfo().apply {
        packageName = PACKAGE
        name = "$PACKAGE.MainActivity"
        enabled = componentEnabled
        applicationInfo = newApplicationInfo(appEnabled, suspended)
    }

    private fun newApplicationInfo(enabled: Boolean, suspended: Boolean = false) =
        ApplicationInfo().apply {
            packageName = PACKAGE
            this.enabled = enabled
            flags = if (suspended) ApplicationInfo.FLAG_SUSPENDED else 0
        }

    private fun call(name: String, vararg args: Any?): Any? {
        val method = OnDemandUnfreezeHook::class.java.declaredMethods.single {
            it.name == name && it.parameterCount == args.size
        }.apply { isAccessible = true }
        return method.invoke(hook, *args)
    }

    private class RequestStub(
        @JvmField var activityInfo: ActivityInfo?,
    ) {
        @JvmField
        var resolveInfo: ResolveInfo? = ResolveInfo().apply {
            activityInfo = this@RequestStub.activityInfo
        }
    }

    @Implements(value = XposedHelpers::class, isInAndroidSdk = false)
    class XposedHelpersShadow {
        companion object {
            @Implementation
            @JvmStatic
            fun getObjectField(obj: Any, fieldName: String): Any? =
                obj.javaClass.getDeclaredField(fieldName).run {
                    isAccessible = true
                    get(obj)
                }

            @Implementation
            @JvmStatic
            fun setObjectField(obj: Any, fieldName: String, value: Any?) {
                obj.javaClass.getDeclaredField(fieldName).run {
                    isAccessible = true
                    set(obj, value)
                }
            }
        }
    }

    private companion object {
        private const val PACKAGE = "com.example.frozen"
    }
}
