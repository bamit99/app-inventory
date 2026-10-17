package com.example.appinventory.data

interface StorageSource {
    fun storageBytes(pkg: String): Long
    fun cacheBytes(pkg: String): Long
}
