package com.example.appinventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.Badge

private fun fmtDate(v: Long): String = if (v < 0) "" else SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(v))
private fun fmtNum(v: Long): String = if (v < 0) "" else v.toString()

@Composable
fun AppRow(e: AppEntry, maxStorage: Long, badge: Badge, onUninstall: () -> Unit) {
    val fraction = if (maxStorage <= 0L || e.storageBytes < 0) 0f else (e.storageBytes.toFloat() / maxStorage).coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(e.label, fontWeight = FontWeight.Bold)
                if (badge != Badge.NONE) Text(badge.name, color = MaterialTheme.colorScheme.error)
            }
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Install ${fmtDate(e.installTime)}")
                Text("Last ${fmtDate(e.lastLaunch)}")
                Text("Launches ${fmtNum(e.launchCount)}")
                TextButton(onClick = onUninstall) { Text("Uninstall") }
            }
        }
    }
}
