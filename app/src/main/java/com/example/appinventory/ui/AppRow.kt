package com.example.appinventory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.appinventory.data.AppEntry
import com.example.appinventory.logic.Badge

private fun fmtDate(v: Long): String = if (v < 0) "" else SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(v))
private fun fmtNum(v: Long): String = if (v < 0) "" else v.toString()

@Composable
fun AppRow(e: AppEntry, maxStorage: Long, badge: Badge, onUninstall: () -> Unit) {
    val ctx = LocalContext.current
    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, e.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                ctx.packageManager.getApplicationIcon(e.packageName).toBitmap().asImageBitmap()
            } catch (x: Exception) {
                null
            }
        }
    }
    val fraction = if (maxStorage <= 0L || e.storageBytes < 0) 0f else (e.storageBytes.toFloat() / maxStorage).coerceIn(0f, 1f)
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Image(icon!!, contentDescription = null, modifier = Modifier.size(40.dp).padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(e.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    if (badge != Badge.NONE) Text(badge.name.lowercase(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(5.dp).padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Install ${fmtDate(e.installTime)}", style = MaterialTheme.typography.labelSmall)
                    Text("Last ${fmtDate(e.lastLaunch)}", style = MaterialTheme.typography.labelSmall)
                    Text("Launches ${fmtNum(e.launchCount)}", style = MaterialTheme.typography.labelSmall)
                }
            }
            TextButton(onClick = onUninstall) { Text("Uninstall") }
        }
    }
}