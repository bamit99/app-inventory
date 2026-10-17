package com.example.appinventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.FilterCard
import com.example.appinventory.logic.Rules
import com.example.appinventory.logic.SortColumn

private fun fmtBytes(b: Long): String = if (b < 0) "" else "%.1f MB".format(b / 1_048_576.0)

@Composable
private fun SummaryCard(label: String, value: String, filter: FilterCard, vm: AppViewModel) {
    Column(Modifier.clickable { vm.setFilter(filter) }.padding(8.dp)) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
        Text(label)
    }
}

@Composable
private fun SortHeader(label: String, col: SortColumn, vm: AppViewModel) {
    Text(label, Modifier.clickable { vm.setSort(col) }.padding(8.dp))
}

@Composable
fun InventoryScreen(vm: AppViewModel, onUninstall: (AppEntry) -> Unit) {
    val state by vm.state.collectAsState()
    val now = System.currentTimeMillis()
    val maxStorage = (state.all.maxOfOrNull { it.storageBytes } ?: 1L).coerceAtLeast(1L)
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            SummaryCard("Total", state.counts.total.toString(), FilterCard.ALL, vm)
            SummaryCard("Never opened", state.counts.neverOpened.toString(), FilterCard.NEVER_OPENED, vm)
            SummaryCard("Kill candidates", state.counts.killCandidates.toString(), FilterCard.KILL_CANDIDATES, vm)
            SummaryCard("Reclaimable cache", fmtBytes(state.counts.reclaimableCacheBytes), FilterCard.RECLAIMABLE, vm)
        }
        OutlinedTextField(state.query, { vm.setSearch(it) }, singleLine = true, label = { Text("Search") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth()) {
            SortHeader("Last launch", SortColumn.LAST_LAUNCH, vm)
            SortHeader("Install", SortColumn.INSTALL, vm)
            SortHeader("Launch count", SortColumn.LAUNCH_COUNT, vm)
            SortHeader("Storage", SortColumn.STORAGE, vm)
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(state.visible, key = { it.packageName }) { e ->
                AppRow(e, maxStorage, Rules.badge(e, now), { onUninstall(e) })
            }
        }
    }
}
