package com.example.appinventory.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

interface UsageSource {
    fun launchCount(pkg: String, since: Long): Long
    fun lastLaunch(pkg: String, since: Long): Long
}

// Builds launch count + last-launch in a SINGLE queryEvents pass (lazily, once),
// then answers per-package in O(1). Querying per package (300 Binder calls) is
// what made the first scan take >20s.
//
// Access handling:
// - Without Usage Access the service is non-null but queryEvents returns nothing,
//   so we report -1 (unknown/blank) instead of fabricating a 0.
// - The cache is invalidated whenever the access grant state changes so the
//   denied->granted transition repopulates data instead of serving an empty cache.
class AndroidUsageSource(private val ctx: Context) : UsageSource {
    private val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    // null => not yet cached; volatile-equivalent guarded by the main-thread usage.
    private var cachedGranted: Boolean? = null
    private var cache: Map<String, Pair<Long, Long>>? = null // pkg -> (count, lastResumeMs)

    private fun granted(): Boolean = UsageAccess.hasUsageAccess(ctx)

    private fun ensure(): Map<String, Pair<Long, Long>> {
        val g = granted()
        // Invalidate when the grant state flips (typically denied then granted).
        if (cachedGranted != g) {
            cachedGranted = g
            cache = null
        }
        if (cache != null) return cache!!
        val m = HashMap<String, Pair<Long, Long>>()
        if (g) {
            usm?.let { manager ->
                val from = System.currentTimeMillis() - WINDOW_MS
                val ev = manager.queryEvents(from, System.currentTimeMillis())
                val e = UsageEvents.Event()
                while (ev.hasNextEvent()) {
                    ev.getNextEvent(e)
                    val foreground = e.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                    if (foreground) {
                        val old = m[e.packageName]
                        m[e.packageName] = ((old?.first ?: 0L) + 1L) to e.timeStamp
                    }
                }
            }
            cache = m
        } // denied: leave cache null so we never serve a stale empty map
        return cache ?: emptyMap()
    }

    override fun launchCount(pkg: String, since: Long): Long {
        if (!granted()) return -1L // unknown/no data, not a fabricated 0
        return ensure()[pkg]?.first ?: 0L // access granted + no events => genuinely never opened
    }

    override fun lastLaunch(pkg: String, since: Long): Long {
        if (!granted()) return -1L
        return ensure()[pkg]?.second ?: -1L // never launched => unknown/blank
    }

    private companion object {
        const val WINDOW_MS = 5L * 365L * 24L * 60L * 60L * 1000L
    }
}