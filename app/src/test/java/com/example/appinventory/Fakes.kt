package com.example.appinventory

import com.example.appinventory.data.AppEntry
import com.example.appinventory.data.PackageListSource
import com.example.appinventory.data.StorageSource
import com.example.appinventory.data.UsageSource

class FakePackageListSource(private val entries: List<AppEntry>) : PackageListSource {
    override fun list(): List<AppEntry> = entries
}

class FakeUsageSource(
    private val counts: Map<String, Long>,
    private val lastLaunches: Map<String, Long>,
) : UsageSource {
    override fun launchCount(pkg: String, since: Long): Long = counts[pkg] ?: -1L
    override fun lastLaunch(pkg: String, since: Long): Long = lastLaunches[pkg] ?: -1L
}

class FakeStorageSource(
    private val storage: Map<String, Long>,
    private val cache: Map<String, Long>,
) : StorageSource {
    override fun storageBytes(pkg: String): Long = storage[pkg] ?: -1L
    override fun cacheBytes(pkg: String): Long = cache[pkg] ?: -1L
}
