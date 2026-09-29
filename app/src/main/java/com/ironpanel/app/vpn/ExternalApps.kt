package com.ironpanel.app.vpn

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Handoff targets for protocols without an embedded core on this build.
 * Every package is optional: if it is missing we fall back to clipboard +
 * Play Store, so the app never dead-ends on any device.
 */
object ExternalApps {
    const val WIREGUARD = "com.wireguard.android"
    const val OPENVPN = "de.blinkt.openvpn"
    const val V2RAY_NG = "com.v2ray.ang"
    const val NEKOBOX = "moe.nb4a"
    const val HIDDIFY = "app.hiddify.com"
    const val TELEGRAM = "org.telegram.messenger"

    fun installed(context: Context, pkg: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openPlayStore(context: Context, pkg: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
        Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
    }
}
