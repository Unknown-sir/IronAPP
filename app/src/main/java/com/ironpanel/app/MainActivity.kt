package com.ironpanel.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.ironpanel.app.data.SubLinkParser
import com.ironpanel.app.ui.AppShell
import com.ironpanel.app.ui.AppViewModel
import com.ironpanel.app.ui.IronTheme
import com.ironpanel.app.vpn.Share
import com.ironpanel.app.vpn.VpnManager

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()
    private var crashReport by mutableStateOf<String?>(null)

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        crashReport = CrashReporter.pendingReport(this)
        handleDeepLink(intent)
        setContent {
            val theme by vm.theme.collectAsState(initial = "system")
            IronTheme(theme) {
                Surface(Modifier.fillMaxSize()) {
                    AppShell(vm, onScanQr = { startQrScan() })
                    CrashDialog()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Every foregrounding re-reads used/remaining volume + time left.
        vm.refreshOnOpen()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    /** Accept sub links shared/opened from browsers and messengers. */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.dataString ?: intent?.getStringExtra(Intent.EXTRA_TEXT) ?: return
        if (SubLinkParser.parse(data) != null) {
            vm.import(data) { /* surfaced on next import screen */ }
        }
    }

    private fun startQrScan() {
        IntentIntegrator(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("")
            .setBeepEnabled(false)
            .initiateScan()
    }

    @androidx.compose.runtime.Composable
    private fun CrashDialog() {
        val report = crashReport ?: return
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { crashReport = null; CrashReporter.clear(context) },
            title = { Text("گزارش کرش / Crash report") },
            text = {
                Text(
                    report.take(1200),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Share.copy(context, "Crash report copied", report)
                    crashReport = null
                    CrashReporter.clear(context)
                }) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = {
                    crashReport = null
                    CrashReporter.clear(context)
                }) { Text("OK") }
            }
        )
    }

    @Deprecated("Legacy QR + VPN permission results")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val qr = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (qr != null) {
            qr.contents?.let { vm.import(it) {} }
            return
        }
        if (requestCode == VpnManager.VPN_REQUEST_CODE) {
            VpnManager.onVpnPermissionResult(resultCode == RESULT_OK)
        }
    }
}
