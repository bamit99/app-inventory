package com.example.appinventory.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionGate(hasAccess: Boolean, onGrant: () -> Unit, content: @Composable () -> Unit) {
    Column {
        if (!hasAccess) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Grant Usage Access", style = MaterialTheme.typography.titleLarge)
                Text("Launch counts, storage and cache need usage access.", Modifier.padding(vertical = 8.dp))
                Button(onClick = onGrant) { Text("Grant") }
            }
        }
        content()
    }
}
