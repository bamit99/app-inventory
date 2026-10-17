package com.example.appinventory

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.example.appinventory.logic.BloatFilter

class BloatFilterTest {
    @Test fun hidesVendorBloat() {
        assertTrue(BloatFilter.isVendor("com.coloros.relax"))
        assertTrue(BloatFilter.isVendor("com.oppo.noteapp"))
        assertTrue(BloatFilter.isVendor("com.oplus.consumerIRApp"))
        assertTrue(BloatFilter.isVendor("com.heytap.pictorial"))
    }
    @Test fun keepsRealUserApps() {
        assertFalse(BloatFilter.isVendor("com.google.android.youtube"))
        assertFalse(BloatFilter.isVendor("com.spotify.music"))
        assertFalse(BloatFilter.isVendor("com.whatsapp"))
        assertFalse(BloatFilter.isVendor("com.example.myapp"))
        assertFalse(BloatFilter.isVendor("com.opera.browser"))
    }
    @Test fun prefixMatchIsSegmentAware() {
        // "com.oplusfoo" must NOT match "com.oplus." prefix
        assertFalse(BloatFilter.isVendor("com.oplusfoo.bar"))
    }
}