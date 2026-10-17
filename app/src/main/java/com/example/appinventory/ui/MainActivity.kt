package com.example.appinventory.ui

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.appinventory.data.AndroidPackageListSource
import com.example.appinventory.data.AndroidStorageSource
import com.example.appinventory.data.AndroidUsageSource
import com.example.appinventory.data.AppEntry
import com.example.appinventory.data.AppInventoryRepository
import com.example.appinventory.data.UsageAccess
import kotlinx.coroutines.launch
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
                DisposableEffect(Unit) {
                    val obs = LifecycleEventObserver { _, e ->
                        if (e == Lifecycle.Event.ON_RESUME) hasAccess = UsageAccess.hasUsageAccess(this@MainActivity)
                    }
                    lifecycle.addObserver(obs)
                    onDispose { lifecycle.removeObserver(obs) }
                }
                val scope = rememberCoroutineScope()
                LaunchedEffect(hasAccess) { if (hasAccess) vm.refresh() }
                // Auto-refresh the list when the system reports a package uninstalled.
                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(c: Context?, i: Intent?) {
                            if (i?.action == Intent.ACTION_PACKAGE_REMOVED) scope.launch { vm.refresh() }
                        }
                    }
                    val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply { addDataScheme("package") }
                    ContextCompat.registerReceiver(this@MainActivity, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                    onDispose { this@MainActivity.unregisterReceiver(receiver) }
                }
                Screen(vm, hasAccess, ::exportCsv)
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

// ACTION_DELETE's UninstallerActivity launches but renders nothing visible on some
// OEM builds (ColorOS). PackageInstaller.uninstall() shows the real system confirmation.
// This ColorOS build suppresses uninstall-confirmation dialogs for both
// ACTION_DELETE and ACTION_UNINSTALL_PACKAGE (they launch but render nothing).
// The app-details Settings page always shows, and has a visible Uninstall button.
private fun requestUninstall(ctx: Context, pkg: String) {
    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(vm: AppViewModel, hasAccess: Boolean, onExport: (AppViewModel) -> Unit) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Inventory") },
                actions = {
                    TextButton(onClick = { scope.launch { vm.refresh() } }) { Text("Scan") }
                    TextButton(onClick = { onExport(vm) }) { Text("CSV") }
                },
            )
        },
    ) { pad ->
        PermissionGate(hasAccess, onGrant = { activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
            Column(Modifier.fillMaxSize().padding(pad)) {
                InventoryScreen(vm, onUninstall = { e: AppEntry -> requestUninstall(activity, e.packageName) })
            }
        }
    }
}