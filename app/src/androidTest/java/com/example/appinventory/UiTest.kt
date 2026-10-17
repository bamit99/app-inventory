package com.example.appinventory

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import com.example.appinventory.data.AppEntry
import com.example.appinventory.data.AppInventoryRepository
import com.example.appinventory.data.PackageListSource
import com.example.appinventory.data.StorageSource
import com.example.appinventory.data.UsageSource
import com.example.appinventory.ui.AppViewModel
import com.example.appinventory.ui.InventoryScreen

private class TList(val entries: List<AppEntry>) : PackageListSource { override fun list() = entries }
private class TUsage(val counts: Map<String, Long>, val last: Map<String, Long>) : UsageSource {
    override fun launchCount(pkg: String, since: Long) = counts[pkg] ?: -1L
    override fun lastLaunch(pkg: String, since: Long) = last[pkg] ?: -1L
}
private class TStorage(val storage: Map<String, Long>, val cache: Map<String, Long>) : StorageSource {
    override fun storageBytes(pkg: String) = storage[pkg] ?: -1L
    override fun cacheBytes(pkg: String) = cache[pkg] ?: -1L
}

@RunWith(AndroidJUnit4::class)
class UiTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun vm(base: List<AppEntry>, usage: TUsage, storage: TStorage) =
        AppViewModel(AppInventoryRepository(TList(base), usage, storage)) { System.currentTimeMillis() }

    @Test fun rendersSummaryAndRow() {
        val base = listOf(AppEntry("com.k", "K", 0, 0, 0, -1, 100, 50, true))
        val v = vm(base, TUsage(emptyMap(), emptyMap()), TStorage(mapOf("com.k" to 100L), mapOf("com.k" to 50L)))
        v.refresh()
        composeTestRule.setContent { InventoryScreen(v) { } }
        composeTestRule.onNodeWithText("Kill candidates").assertExists()
        composeTestRule.onNodeWithText("K").assertExists()
    }

    @Test fun unknownFieldShowsBlank() {
        val base = listOf(AppEntry("com.u", "UnknownLabel", 0, 0, -1, -1, -1, -1, true))
        val v = vm(base, TUsage(emptyMap(), emptyMap()), TStorage(emptyMap(), emptyMap()))
        v.refresh()
        composeTestRule.setContent { InventoryScreen(v) { } }
        assertTrue(composeTestRule.onNodeWithText("-1").assertDoesNotExist() != null)
    }
}
