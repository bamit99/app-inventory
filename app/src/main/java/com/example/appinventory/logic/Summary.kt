package com.example.appinventory.logic

import com.example.appinventory.data.AppEntry

data class SummaryCounts(val total: Int, val neverOpened: Int, val killCandidates: Int, val reclaimableCacheBytes: Long)

object Summary {
    fun compute(list: List<AppEntry>, now: Long): SummaryCounts = SummaryCounts(
        total = list.size,
        neverOpened = list.count { it.launchCount == 0L },
        killCandidates = list.count { Rules.isKillCandidate(it, now) },
        reclaimableCacheBytes = list.filter { it.cacheBytes >= 0 }.sumOf { it.cacheBytes },
    )
}
