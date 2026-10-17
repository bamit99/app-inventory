package com.example.appinventory.data

class AppInventoryRepository(
    private val pkg: PackageListSource,
    private val usage: UsageSource,
    private val storage: StorageSource,
) {
    fun load(): List<AppEntry> = pkg.list().map { base ->
        base.copy(
            launchCount = usage.launchCount(base.packageName, base.installTime),
            lastLaunch = usage.lastLaunch(base.packageName, base.installTime),
            storageBytes = storage.storageBytes(base.packageName),
            cacheBytes = storage.cacheBytes(base.packageName),
        )
    }
}
