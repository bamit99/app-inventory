package com.example.appinventory

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.FilterCard
import com.example.appinventory.logic.SortColumn
import com.example.appinventory.logic.SortSearch

class SortSearchTest {
    private val now = 1_700_000_000_000L
    private val day = 86_400_000L
    private val kill = AppEntry("k", "K", 0, 0, 0, -1, 100, 50, true)
    private val active = AppEntry("a", "A", 0, 0, 10, now, 100, 10, true)
    private val stale = AppEntry("s", "S", 0, 0, 4, now - 91 * day, 100, 10, true)
    @Test fun defaultPutsKillFirstThenOldest() {
        val out = SortSearch.apply(listOf(active, kill, stale), SortColumn.LAST_LAUNCH, "", FilterCard.ALL, now)
        assertEquals(listOf("k", "s", "a"), out.map { it.packageName })
    }
    @Test fun filterKillOnly() {
        val out = SortSearch.apply(listOf(active, kill, stale), SortColumn.LAST_LAUNCH, "", FilterCard.KILL_CANDIDATES, now)
        assertEquals(listOf("k", "s"), out.map { it.packageName })
    }
    @Test fun searchMatchesLabel() {
        val out = SortSearch.apply(listOf(active, kill), SortColumn.LAST_LAUNCH, "act", FilterCard.ALL, now)
        assertEquals(listOf("a"), out.map { it.packageName })
    }
    @Test fun emptySafe() { assertTrue(SortSearch.apply(emptyList(), SortColumn.STORAGE, "", FilterCard.ALL, now).isEmpty()) }
}
