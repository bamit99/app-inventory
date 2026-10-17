package com.example.appinventory.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

interface PackageListSource {
    fun list(): List<AppEntry>
}

class AndroidPackageListSource(private val ctx: Context) : PackageListSource {
    override fun list(): List<AppEntry> {
        val pm = ctx.packageManager
        val self = ctx.packageName
        return pm.getInstalledPackages(0)
            .filter { it.packageName != self }
            .filter { (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) == 0 }
            .filter { !com.example.appinventory.logic.BloatFilter.isVendor(it.packageName) }
            .map { pi ->
                AppEntry(
                    packageName = pi.packageName,
                    label = pi.applicationInfo?.loadLabel(pm)?.toString() ?: pi.packageName,
                    installTime = pi.firstInstallTime,
                    lastUpdateTime = pi.lastUpdateTime,
                    launchCount = -1,
                    lastLaunch = -1,
                    storageBytes = -1,
                    cacheBytes = -1,
                    removable = pi.applicationInfo?.enabled ?: false,
                )
            }
    }
}
