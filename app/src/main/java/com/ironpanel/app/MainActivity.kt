package com.ironpanel.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.zxing.integration.android.IntentIntegrator
import com.ironpanel.app.data.SubLinkParser
import com.ironpanel.app.ui.AppShell
import com.ironpanel.app.ui.AppViewModel
import com.ironpanel.app.ui.IronTheme
import com.ironpanel.app.vpn.VpnManager

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VpnManager.snapshotRefContext = this
        handleDeepLink(intent)
        setContent {
            val theme by vm.theme.collectAsState(initial = "system")
            IronTheme(theme) {
                Surface(Modifier.fillMaxSize()) {
                    AppShell(vm, onScanQr = { startQrScan() })
                }
            }
        }
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
