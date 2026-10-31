package com.aistra.hail.utils

import android.content.Intent
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.api.ApiActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = HailApp::class)
class HShortcutsTest {
    @Test
    fun launchShortcutIntentTargetsApiActivityWithPackage() {
        val intent = HShortcuts.launchShortcutIntent("com.example.app")

        assertEquals(HailApi.ACTION_LAUNCH, intent.action)
        assertEquals(ApiActivity::class.java.name, intent.component?.className)
        assertEquals("com.example.app", intent.getStringExtra(HailData.KEY_PACKAGE))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
