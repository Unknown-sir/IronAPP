package com.ironpanel.app.vpn

import android.content.Context
import android.content.Intent
import com.ironpanel.app.data.AppSnapshot
import com.ironpanel.app.vpn.box.buildConfig

/**
 * Every tappable protocol runs through the embedded sing-box core inside
 * IronAPP's own VpnService. No third-party client is ever downloaded.
 */
object SingBoxConnector {

    /** Protocols with a real in-app engine. Everything else is view-only. */
    val ENGINE_PROTOCOLS = setOf(
        "xray", "wireguard", "hysteria2", "ssh", "openvpn", "ocserv"
    )

    fun connect(context: Context, snapshot: AppSnapshot, protocol: String, linkIndex: Int = 0) {
        val config = buildConfig(snapshot, protocol, linkIndex)
        connectRaw(context, config, "$protocol · ${snapshot.user.username}")
    }

    fun connectRaw(context: Context, config: String, label: String) {
        val start = Intent(context, IronVpnService::class.java)
            .setAction(IronVpnService.ACTION_START)
            .putExtra(IronVpnService.EXTRA_CONFIG, config)
            .putExtra(IronVpnService.EXTRA_LABEL, label)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(start)
        } else {
            context.startService(start)
        }
        TrafficMonitor.start(context)
    }

    fun disconnect(context: Context) {
        TrafficMonitor.stop()
        context.startService(
            Intent(context, IronVpnService::class.java)
                .setAction(IronVpnService.ACTION_STOP)
        )
    }
}
