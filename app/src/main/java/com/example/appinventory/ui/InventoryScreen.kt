package com.example.appinventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.FilterCard
import com.example.appinventory.logic.Rules
import com.example.appinventory.logic.SortColumn

private fun fmtBytes(b: Long): String = if (b < 0) "" else "%.1f MB".format(b / 1_048_576.0)

@Composable
private fun StatCard(label: String, value: String, filter: FilterCard, vm: AppViewModel, active: Boolean, modifier: Modifier = Modifier) {
    val colors = if (active) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
    Card(modifier.clickable { vm.setFilter(filter) }, colors = colors) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun InventoryScreen(vm: AppViewModel, onUninstall: (AppEntry) -> Unit) {
    val state by vm.state.collectAsState()
    val now = System.currentTimeMillis()
    val maxStorage = (state.all.maxOfOrNull { it.storageBytes } ?: 1L).coerceAtLeast(1L)
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Total", state.counts.total.toString(), FilterCard.ALL, vm, state.filter == FilterCard.ALL, Modifier.weight(1f))
                StatCard("Unused", state.counts.neverOpened.toString(), FilterCard.NEVER_OPENED, vm, state.filter == FilterCard.NEVER_OPENED, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Cleanup", state.counts.killCandidates.toString(), FilterCard.KILL_CANDIDATES, vm, state.filter == FilterCard.KILL_CANDIDATES, Modifier.weight(1f))
                StatCard("Cache", fmtBytes(state.counts.reclaimableCacheBytes), FilterCard.RECLAIMABLE, vm, state.filter == FilterCard.RECLAIMABLE, Modifier.weight(1f))
            }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = { vm.setSearch(it) },
            singleLine = true,
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(state.sort == SortColumn.LAST_LAUNCH, { vm.setSort(SortColumn.LAST_LAUNCH) }, { Text("Last launch") })
            FilterChip(state.sort == SortColumn.INSTALL, { vm.setSort(SortColumn.INSTALL) }, { Text("Install") })
            FilterChip(state.sort == SortColumn.LAUNCH_COUNT, { vm.setSort(SortColumn.LAUNCH_COUNT) }, { Text("Launches") })
            FilterChip(state.sort == SortColumn.STORAGE, { vm.setSort(SortColumn.STORAGE) }, { Text("Storage") })
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.loading && state.all.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Scanning apps…", Modifier.padding(top = 12.dp))
                }
                state.visible.isEmpty() -> Text("No apps to show.\nTap Scan / Refresh to build the list.", style = MaterialTheme.typography.bodyLarge)
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.visible, key = { it.packageName }) { e ->
                        AppRow(e, maxStorage, Rules.badge(e, now), { onUninstall(e) })
                    }
                }
            }
        }
    }
}