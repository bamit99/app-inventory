package com.example.appinventory.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

interface UsageSource {
    fun launchCount(pkg: String, since: Long): Long
    fun lastLaunch(pkg: String, since: Long): Long
}

class AndroidUsageSource(private val ctx: Context) : UsageSource {
    private val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private fun events(since: Long) = usm?.queryEvents(since, System.currentTimeMillis())

    override fun launchCount(pkg: String, since: Long): Long {
        val ev = events(since) ?: return -1L
        val e = UsageEvents.Event()
        var c = 0L
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.packageName == pkg && e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) c++
        }
        return c
    }

    override fun lastLaunch(pkg: String, since: Long): Long {
        val ev = events(since) ?: return -1L
        val e = UsageEvents.Event()
        var last = -1L
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.packageName == pkg && e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) last = e.timeStamp
        }
        return last
    }
}
