package com.example.appinventory

import org.junit.Test
import kotlin.test.assertEquals
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.CsvExport

class CsvExportTest {
    @Test fun headerMatchesColumns() {
        assertEquals(listOf("label", "package", "install", "lastLaunch", "launchCount", "storageBytes", "cacheBytes"), CsvExport.HEADER)
    }
    @Test fun unknownFieldsExportEmpty() {
        val e = AppEntry("pkg", "Label", 0, 0, -1, -1, -1, -1, true)
        val row = CsvExport.rows(listOf(e)).single()
        assertEquals("", row[3]); assertEquals("", row[4]); assertEquals("", row[5]); assertEquals("", row[6])
    }
    @Test fun writeHasHeaderPlusRows() {
        val csv = CsvExport.write(listOf(AppEntry("p", "L", 0, 0, 5, 10, 20, 3, true)))
        assertEquals(CsvExport.HEADER.size, csv.lineSequence().first().split(",").size)
        assertEquals(2, csv.trim().lines().size)
    }
}
