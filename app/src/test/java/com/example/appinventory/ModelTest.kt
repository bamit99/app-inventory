package com.example.appinventory

import org.junit.Test
import kotlin.test.assertEquals
import com.example.appinventory.data.AppEntry

class ModelTest {
    @Test fun entryDefaultsUnknownToMinusOne() {
        val e = AppEntry("x", "X", 0, 0, -1, -1, -1, -1, true)
        assertEquals(-1L, e.launchCount)
        assertEquals(-1L, e.lastLaunch)
    }
}
