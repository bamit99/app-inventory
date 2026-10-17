package com.example.appinventory

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.Badge
import com.example.appinventory.logic.Rules

class RulesTest {
    private val now = 1_700_000_000_000L
    private val day = 86_400_000L
    @Test fun zeroLaunchIsKill() {
        val e = AppEntry("a", "A", 0, 0, 0, -1, 0, 0, true)
        assertTrue(Rules.isKillCandidate(e, now))
        assertEquals(Badge.NEVER, Rules.badge(e, now))
    }
    @Test fun fewLaunchesLongAgoIsKill() {
        val e = AppEntry("b", "B", 0, 0, 4, now - 91 * day, 0, 0, true)
        assertTrue(Rules.isKillCandidate(e, now))
        assertEquals(Badge.STALE, Rules.badge(e, now))
    }
    @Test fun unknownLaunchNotKill() {
        val e = AppEntry("c", "C", 0, 0, -1, -1, 0, 0, true)
        assertFalse(Rules.isKillCandidate(e, now))
        assertEquals(Badge.NONE, Rules.badge(e, now))
    }
    @Test fun activeAppNotKill() {
        val e = AppEntry("d", "D", 0, 0, 100, now - day, 0, 0, true)
        assertFalse(Rules.isKillCandidate(e, now))
    }
}
