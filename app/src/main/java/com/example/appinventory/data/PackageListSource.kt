package com.example.appinventory.data

interface PackageListSource {
    fun list(): List<AppEntry>
}
