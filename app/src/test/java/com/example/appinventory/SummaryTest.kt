package com.example.appinventory

import org.junit.Test
import kotlin.test.assertEquals
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.Summary

class SummaryTest {
    private val now = 1_700_000_000_000L
    @Test fun emptyListIsAllZeros() {
        val s = Summary.compute(emptyList(), now)
        assertEquals(0, s.total); assertEquals(0L, s.reclaimableCacheBytes)
    }
    @Test fun countsAndCache() {
        val never = AppEntry("a", "A", 0, 0, 0, -1, 100, 50, true)
        val known = AppEntry("b", "B", 0, 0, 100, now, 200, 30, true)
        val unknown = AppEntry("c", "C", 0, 0, -1, -1, -1, -1, true)
        val s = Summary.compute(listOf(never, known, unknown), now)
        assertEquals(3, s.total); assertEquals(1, s.neverOpened)
        assertEquals(1, s.killCandidates); assertEquals(80L, s.reclaimableCacheBytes)
    }
}
