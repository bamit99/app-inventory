package com.example.appinventory.data

data class AppEntry(
    val packageName: String,
    val label: String,
    val installTime: Long,
    val lastUpdateTime: Long,
    val launchCount: Long,
    val lastLaunch: Long,
    val storageBytes: Long,
    val cacheBytes: Long,
    val removable: Boolean,
)
