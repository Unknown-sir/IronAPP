package com.ironpanel.app.vpn

import android.content.Context
import android.content.Intent
import com.ironpanel.app.data.AppSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

/**
 * In-app WireGuard via the official embeddable tunnel library
 * (com.wireguard.android:tunnel, Apache-2.0). Falls back to the official
 * WireGuard app when the embedded core is disabled or fails.
 */
object WireGuardConnector {

    suspend fun connect(context: Context, snapshot: AppSnapshot): Result<String> =
        withContext(Dispatchers.IO) {
            val conf = snapshot.user.configs["wireguard.conf"].orEmpty()
            if (conf.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No wireguard.conf for this user"))
            }
            try {
                startEmbedded(context, conf)
                Result.success("WireGuard")
            } catch (e: Exception) {
                // Embedded core unavailable on this build/device → handoff.
                handoffToApp(context, conf)
                Result.success("WireGuard (external app)")
            }
        }

    private fun startEmbedded(context: Context, conf: String) {
        val backend = com.wireguard.android.backend.GoBackend(context.applicationContext)
        val config = ByteArrayInputStream(conf.toByteArray(Charsets.UTF_8)).use { input ->
            com.wireguard.config.Config.parse(input)
        }
        val tunnel = object : com.wireguard.android.backend.Tunnel {
            override fun getName(): String = "ironapp-wg"
            override fun onStateChange(newState: com.wireguard.android.backend.Tunnel.State) = Unit
        }
        backend.setState(tunnel, com.wireguard.android.backend.Tunnel.State.UP, config)
        WgHolder.backend = backend
        WgHolder.tunnel = tunnel
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            val backend = WgHolder.backend
            val tunnel = WgHolder.tunnel
            if (backend != null && tunnel != null) {
                backend.setState(tunnel, com.wireguard.android.backend.Tunnel.State.DOWN, null)
            }
        } catch (_: Exception) {
        } finally {
            WgHolder.backend = null
            WgHolder.tunnel = null
        }
    }

    private fun handoffToApp(context: Context, conf: String) {
        if (ExternalApps.installed(context, ExternalApps.WIREGUARD)) {
            // The official app imports text/plain tunnel configs via SEND.
            val send = Intent(Intent.ACTION_SEND)
                .setPackage(ExternalApps.WIREGUARD)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, conf)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(send)
        } else {
            ExternalApps.copyToClipboard(context, "WireGuard config copied", conf)
            ExternalApps.openPlayStore(context, ExternalApps.WIREGUARD)
        }
    }

    private object WgHolder {
        var backend: com.wireguard.android.backend.GoBackend? = null
        var tunnel: com.wireguard.android.backend.Tunnel? = null
    }
}
