package com.example.appinventory.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.appinventory.data.AndroidPackageListSource
import com.example.appinventory.data.AndroidStorageSource
import com.example.appinventory.data.AndroidUsageSource
import com.example.appinventory.data.AppEntry
import com.example.appinventory.data.AppInventoryRepository
import com.example.appinventory.data.UsageAccess
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = AppViewModel(
            AppInventoryRepository(AndroidPackageListSource(this), AndroidUsageSource(this), AndroidStorageSource(this)),
        ) { System.currentTimeMillis() }
        setContent {
            MaterialTheme {
                var hasAccess by remember { mutableStateOf(UsageAccess.hasUsageAccess(this@MainActivity)) }
                LaunchedEffect(hasAccess) { if (hasAccess) vm.refresh() }
                Scaffold(floatingActionButton = {
                    FloatingActionButton(onClick = { exportCsv(vm) }) { Text("CSV") }
                }) { pad ->
                    Column(Modifier.padding(pad)) {
                        PermissionGate(hasAccess, onGrant = {
                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }) {
                            InventoryScreen(vm, onUninstall = { e: AppEntry ->
                                startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${e.packageName}")))
                            })
                        }
                    }
                }
            }
        }
    }

    private fun exportCsv(vm: AppViewModel) {
        val dir = getExternalFilesDir(null) ?: filesDir
        val out = File(dir, "apps.csv")
        out.writeText(vm.exportCsv())
        Toast.makeText(this, "Exported: ${out.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
