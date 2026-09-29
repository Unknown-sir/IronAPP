package com.ironpanel.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import com.ironpanel.app.MainActivity

/**
 * The app's VpnService. Embedded cores (WireGuard via the tunnel library)
 * park their tun fd here; handoff protocols use it for state + status polling.
 */
class IronVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.ironpanel.app.vpn.START"
        const val ACTION_STOP = "com.ironpanel.app.vpn.STOP"
        const val EXTRA_LABEL = "label"
        const val CHANNEL_ID = "ironapp_vpn"
        const val NOTIF_ID = 1001

        @Volatile
        var runningLabel: String? = null
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                runningLabel = intent.getStringExtra(EXTRA_LABEL)
                startForegroundCompat()
            }
            ACTION_STOP -> {
                runningLabel = null
                VpnManager.onServiceStopped()
                stopForegroundCompat()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        ensureChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = PendingIntent.getActivity(this, 0, openIntent, flags)
        val notif = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("IronAPP")
                .setContentText(runningLabel ?: "")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pending)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("IronAPP")
                .setContentText(runningLabel ?: "")
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
        runningLabel = null
        VpnManager.onServiceStopped()
        stopForegroundCompat()
        stopSelf()
    }
}
