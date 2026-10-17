package com.example.appinventory

import org.junit.Test
import kotlin.test.assertEquals
import com.example.appinventory.data.AppEntry
import com.example.appinventory.data.AppInventoryRepository

class RepositoryTest {
    @Test fun mergesAllSources() {
        val base = AppEntry("com.x", "X", 1000, 1000, -1, -1, -1, -1, true)
        val pkg = FakePackageListSource(listOf(base))
        val usage = FakeUsageSource(mapOf("com.x" to 42L), mapOf("com.x" to 9999L))
        val storage = FakeStorageSource(mapOf("com.x" to 500L), mapOf("com.x" to 100L))
        val e = AppInventoryRepository(pkg, usage, storage).load().single()
        assertEquals(42L, e.launchCount); assertEquals(9999L, e.lastLaunch)
        assertEquals(500L, e.storageBytes); assertEquals(100L, e.cacheBytes)
    }
    @Test fun missingUsageStaysUnknown() {
        val base = AppEntry("com.y", "Y", 1000, 1000, -1, -1, -1, -1, true)
        val pkg = FakePackageListSource(listOf(base))
        val e = AppInventoryRepository(pkg, FakeUsageSource(emptyMap(), emptyMap()), FakeStorageSource(emptyMap(), emptyMap())).load().single()
        assertEquals(-1L, e.launchCount); assertEquals(-1L, e.cacheBytes)
    }
}
