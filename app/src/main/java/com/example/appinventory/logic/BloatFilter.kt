package com.example.appinventory.logic

object BloatFilter {
    private val VENDOR_PREFIXES = listOf(
        "com.oppo.",
        "com.coloros.",
        "com.oplus.",
        "com.heytap.",
        "com.realme.",
        "com.mediatek.",
        "com.myos.",
        "com.iqoo.",
    )
    fun isVendor(pkg: String): Boolean = VENDOR_PREFIXES.any { pkg.startsWith(it) }
}