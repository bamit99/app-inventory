package com.example.appinventory.logic

import com.example.appinventory.data.AppEntry

enum class Badge { NONE, NEVER, STALE }

object Rules {
    const val MAX_LAUNCHES = 5L
    const val STALE_DAYS = 90L
    private const val DAY = 86_400_000L

    fun isKillCandidate(e: AppEntry, now: Long): Boolean =
        e.launchCount == 0L ||
            (e.launchCount in 1 until MAX_LAUNCHES &&
                e.lastLaunch >= 0 && now - e.lastLaunch > STALE_DAYS * DAY)

    fun badge(e: AppEntry, now: Long): Badge = when {
        e.launchCount == 0L -> Badge.NEVER
        isKillCandidate(e, now) -> Badge.STALE
        else -> Badge.NONE
    }
}
