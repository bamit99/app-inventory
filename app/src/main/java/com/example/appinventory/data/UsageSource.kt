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
class AndroidUsageSource(private val ctx: Context) : UsageSource {
    private val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private var cache: Map<String, Pair<Long, Long>>? = null // pkg -> (count, lastResumeMs)

    private fun ensure(): Map<String, Pair<Long, Long>> {
        cache?.let { return it }
        val m = HashMap<String, Pair<Long, Long>>()
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
        return m
    }

    override fun launchCount(pkg: String, since: Long): Long {
        if (usm == null) return -1L
        return ensure()[pkg]?.first ?: 0L // access granted + never launched => genuinely zero
    }

    override fun lastLaunch(pkg: String, since: Long): Long {
        if (usm == null) return -1L
        return ensure()[pkg]?.second ?: -1L // never launched => unknown/blank
    }

    private companion object {
        const val WINDOW_MS = 5L * 365L * 24L * 60L * 60L * 1000L
    }
}