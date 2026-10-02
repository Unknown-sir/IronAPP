package com.ironpanel.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import com.ironpanel.app.BuildConfig
import com.ironpanel.app.MainActivity
import com.ironpanel.app.vpn.box.BoxHost
import com.ironpanel.app.vpn.box.BoxTun
import com.ironpanel.app.vpn.box.IronPlatformInterface
import com.ironpanel.libbox.TunOptions
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The app's single VpnService. It hosts the embedded sing-box core
 * (CommandServer + platform TUN) — every in-app protocol runs here,
 * so nothing ever leaves IronAPP to a third-party client.
 */
class IronVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.ironpanel.app.vpn.START"
        const val ACTION_STOP = "com.ironpanel.app.vpn.STOP"
        const val EXTRA_CONFIG = "config_json"
        const val EXTRA_LABEL = "label"
        const val CHANNEL_ID = "ironapp_vpn"
        const val NOTIF_ID = 1001

        @Volatile
        var runningLabel: String? = null
            private set
    }

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, e ->
                // Nothing on this path may ever kill the process silently:
                // surface it as a readable connect error instead.
                VpnManager.onCoreFailed(e.message ?: "core error")
                stopSelf()
            }
    )
    private var box: BoxHost? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val config = intent.getStringExtra(EXTRA_CONFIG).orEmpty()
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                if (config.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                runningLabel = label
                startForegroundCompat(label)
                scope.launch {
                    try {
                        val host = BoxHost(
                            applicationContext,
                            IronPlatformInterface(this@IronVpnService, applicationContext)
                        )
                        host.start(config)
                        box = host
                        VpnManager.onCoreStarted()
                    } catch (e: Exception) {
                        VpnManager.onCoreFailed(e.message ?: "core start failed")
                        stopSelf()
                    }
                }
            }
            ACTION_STOP -> shutdown()
        }
        return START_STICKY
    }

    /**
     * Build the platform TUN fd for the core. Builder() is an inner class,
     * so this must live in the VpnService subclass itself.
     * Fully guarded: any failure becomes a clean core error, and every step
     * leaves a breadcrumb so a native death is attributable.
     */
    fun createTun(options: TunOptions): Int {
        val ctx = applicationContext
        com.ironpanel.app.vpn.box.BoxCrumbs.mark(ctx, "40-tun-enter")
        try {
            var builder = Builder()
                .setSession("IronAPP")
                .setMtu(options.mtu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder = builder.setMetered(false)
            }
            // Never route our own app through the tunnel (UI + panel API stay direct).
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {
            }
            val v4 = options.inet4Address
            while (v4.hasNext()) {
                val a = v4.next()
                builder.addAddress(a.address(), a.prefix())
            }
        val v6addrs = mutableListOf<Pair<String, Int>>()
        try {
            val v6 = options.inet6Address
            while (v6.hasNext()) {
                val a = v6.next()
                v6addrs.add(a.address() to a.prefix())
            }
        } catch (_: Exception) {
        }
        for ((addr, prefix) in v6addrs) {
            try {
                builder.addAddress(addr, prefix)
            } catch (_: Exception) {
            }
        }
        if (options.autoRoute) {
            try {
                val dns = options.dnsServerAddress
                while (dns.hasNext()) builder.addDnsServer(dns.next())
            } catch (_: Exception) {
            }
            // Full-tunnel routes; our own package is excluded above.
            // ::/0 only when the core actually uses IPv6 (SFA parity).
            builder.addRoute("0.0.0.0", 0)
            if (v6addrs.isNotEmpty()) {
                try {
                    builder.addRoute("::", 0)
                } catch (_: Exception) {
                }
            }
        }
            com.ironpanel.app.vpn.box.BoxCrumbs.mark(ctx, "41-tun-establish")
            val pfd = builder.establish()
                ?: throw IllegalStateException("VpnService not prepared or revoked")
            BoxTun.fd = pfd
            com.ironpanel.app.vpn.box.BoxCrumbs.mark(ctx, "42-tun-fd:" + pfd.fd)
            return pfd.fd
        } catch (e: Exception) {
            com.ironpanel.app.vpn.box.BoxCrumbs.mark(
                ctx, "41-tun-fail:" + (e.message ?: e.javaClass.simpleName)
            )
            throw IllegalStateException("tun failed: ${e.message}")
        }
    }

    private fun shutdown() {        runningLabel = null
        try {
            box?.stop()
        } catch (_: Exception) {
        }
        box = null
        BoxTun.close()
        VpnManager.onServiceStopped()
        stopForegroundCompat()
        stopSelf()
    }

    private fun startForegroundCompat(label: String) {
        ensureChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = PendingIntent.getActivity(this, 0, openIntent, flags)
        val notif = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("IronAPP — Protected")
                .setContentText(label)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pending)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("IronAPP — Protected")
                .setContentText(label)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pending)
                .build()
        }
        startForeground(NOTIF_ID, notif)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "IronAPP VPN",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
    }

    override fun onRevoke() {
        shutdown()
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }
}
