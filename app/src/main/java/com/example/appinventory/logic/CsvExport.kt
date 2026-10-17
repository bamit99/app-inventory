package com.example.appinventory.logic

import com.example.appinventory.data.AppEntry

object CsvExport {
    val HEADER: List<String> = listOf("label", "package", "install", "lastLaunch", "launchCount", "storageBytes", "cacheBytes")

    private fun num(v: Long): String = if (v < 0) "" else v.toString()

    private fun quote(f: String): String = if (f.contains(',')) "\"" + f.replace("\"", "\"\"") + "\"" else f

    fun rows(list: List<AppEntry>): List<List<String>> = list.map { e ->
        listOf(e.label, e.packageName, num(e.installTime), num(e.lastLaunch), num(e.launchCount), num(e.storageBytes), num(e.cacheBytes))
    }

    fun write(list: List<AppEntry>): String {
        val lines = ArrayList<List<String>>()
        lines.add(HEADER)
        lines.addAll(rows(list))
        return lines.joinToString("\n") { row -> row.joinToString(",") { quote(it) } }
    }
}
