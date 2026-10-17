package com.example.appinventory.data

class AppInventoryRepository(
    private val pkg: PackageListSource,
    private val usage: UsageSource,
    private val storage: StorageSource,
) {
    // Source returning -1 means "no data"; never clobber a known base value with it.
    private fun fill(base: Long, src: Long): Long = if (src >= 0) src else base

    fun load(): List<AppEntry> = pkg.list().map { base ->
        base.copy(
            launchCount = fill(base.launchCount, usage.launchCount(base.packageName, base.installTime)),
            lastLaunch = fill(base.lastLaunch, usage.lastLaunch(base.packageName, base.installTime)),
            storageBytes = fill(base.storageBytes, storage.storageBytes(base.packageName)),
            cacheBytes = fill(base.cacheBytes, storage.cacheBytes(base.packageName)),
        )
    }
}
