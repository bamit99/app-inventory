package com.example.appinventory

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import com.example.appinventory.data.AndroidPackageListSource
import com.example.appinventory.data.UsageAccess

@RunWith(AndroidJUnit4::class)
class SystemSourcesSmokeTest {
    @Test fun listsThirdPartyPackages() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val list = AndroidPackageListSource(ctx).list()
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.packageName.isNotEmpty() })
    }
    @Test fun permissionCheckReturnsBoolean() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        UsageAccess.hasUsageAccess(ctx)
    }
}
