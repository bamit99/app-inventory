package com.example.appinventory.data

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Process
import android.os.storage.StorageManager

interface StorageSource {
    fun storageBytes(pkg: String): Long
    fun cacheBytes(pkg: String): Long
}

class AndroidStorageSource(private val ctx: Context) : StorageSource {
    private val ssm = ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager

    private fun stats(pkg: String): StorageStats? {
        val ssm = ssm ?: return null
        return try {
            ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg, Process.myUserHandle())
        } catch (e: Exception) {
            null
        }
    }

    override fun storageBytes(pkg: String): Long {
        val s = stats(pkg) ?: return -1L
        return s.appBytes + s.dataBytes + s.cacheBytes
    }
    override fun cacheBytes(pkg: String): Long = stats(pkg)?.cacheBytes ?: -1L
}
