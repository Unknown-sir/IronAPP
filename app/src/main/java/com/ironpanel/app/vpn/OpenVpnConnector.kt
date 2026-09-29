package com.ironpanel.app.vpn

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ironpanel.app.data.AppSnapshot
import java.io.File

/**
 * OpenVPN connector: writes THIS user's *.ovpn to private storage and hands
 * it to ics-openvpn (de.blinkt.openvpn) via its public import intent.
 * Falls back to clipboard + Play Store when the client is missing.
 */
object OpenVpnConnector {

    suspend fun connect(context: Context, snapshot: AppSnapshot): Result<String> {
        val entry = snapshot.user.configs.entries.firstOrNull { it.key.endsWith(".ovpn") }
            ?: return Result.failure(IllegalStateException("No OpenVPN profile for this user"))
        return try {
            val dir = File(context.filesDir, "profiles").apply { mkdirs() }
            val file = File(dir, "ironapp.ovpn")
            file.writeText(entry.value)
            if (ExternalApps.installed(context, ExternalApps.OPENVPN)) {
                val uri = FileProvider.getUriForFile(
                    context, context.packageName + ".files", file
                )
                val import = Intent("android.intent.action.VIEW")
                    .setDataAndType(uri, "application/x-openvpn-profile")
                    .setPackage(ExternalApps.OPENVPN)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(import)
                Result.success("OpenVPN (external app)")
            } else {
                ExternalApps.copyToClipboard(context, "OpenVPN profile saved", entry.value)
                ExternalApps.openPlayStore(context, ExternalApps.OPENVPN)
                Result.success("OpenVPN (app required)")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnect() {
        // External-client mode holds no socket; state is cleared by VpnManager.
    }
}
