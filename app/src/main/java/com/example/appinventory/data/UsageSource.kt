package com.example.appinventory.data

interface UsageSource {
    fun launchCount(pkg: String, since: Long): Long
    fun lastLaunch(pkg: String, since: Long): Long
}
