package com.example.appinventory.logic

import com.example.appinventory.data.AppEntry

enum class SortColumn { LAST_LAUNCH, INSTALL, LAUNCH_COUNT, STORAGE }
enum class FilterCard { ALL, NEVER_OPENED, KILL_CANDIDATES, RECLAIMABLE }

object SortSearch {
    private fun key(e: AppEntry, c: SortColumn): Long = when (c) {
        SortColumn.LAST_LAUNCH -> e.lastLaunch
        SortColumn.INSTALL -> e.installTime
        SortColumn.LAUNCH_COUNT -> e.launchCount
        SortColumn.STORAGE -> e.storageBytes
    }

    // Unknown (-1) sinks to the bottom for explicit column sorts.
    private fun sortKey(e: AppEntry, c: SortColumn): Long =
        if (key(e, c) < 0) Long.MAX_VALUE else key(e, c)

    fun apply(list: List<AppEntry>, sort: SortColumn, query: String, filter: FilterCard, now: Long): List<AppEntry> {
        var l = list
        if (query.isNotEmpty())
            l = l.filter { it.label.contains(query, true) || query.contains(it.label, true) }
        l = when (filter) {
            FilterCard.ALL -> l
            FilterCard.NEVER_OPENED -> l.filter { it.launchCount == 0L }
            FilterCard.KILL_CANDIDATES -> l.filter { Rules.isKillCandidate(it, now) }
            FilterCard.RECLAIMABLE -> l.filter { it.cacheBytes >= 0 }
        }
        return if (sort == SortColumn.LAST_LAUNCH) {
            val (kill, rest) = l.partition { Rules.isKillCandidate(it, now) }
            (kill + rest).sortedBy { key(it, sort) }
        } else {
            l.sortedBy { sortKey(it, sort) }
        }
    }
}
