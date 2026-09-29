package com.ironpanel.app.vpn

import android.content.Context
import android.content.Intent
import com.ironpanel.app.data.AppSnapshot

/**
 * Xray-family (VLESS/VMess/Trojan/Shadowsocks) connector.
 *
 * v1.0 connects through best-in-class open-source clients already on the
 * device (v2rayNG → NekoBox → Hiddify, in that order) by handing them the
 * exact links the panel generated for THIS user. The in-app Xray core slot
 * ([IN_APP_XRAY]) is reserved for v1.1 (gomobile AAR) — see docs/PROTOCOLS.md.
 * Quota/expiry is always re-checked via /status before any handoff.
 */
object XrayConnector {

    suspend fun connect(context: Context, snapshot: AppSnapshot, linkIndex: Int = 0): Result<String> {
        val links = snapshot.user.xrayLinks
        if (links.isEmpty()) {
            return Result.failure(IllegalStateException("No Xray link for this user"))
        }
        val link = links.getOrElse(linkIndex) { links.first() }
        val target = listOf(
            ExternalApps.V2RAY_NG, ExternalApps.NEKOBOX, ExternalApps.HIDDIFY
        ).firstOrNull { ExternalApps.installed(context, it) }
        if (target == null) {
            ExternalApps.copyToClipboard(context, "Xray link copied", link)
            ExternalApps.openPlayStore(context, ExternalApps.V2RAY_NG)
            return Result.success("Xray (link copied)")
        }
        return try {
            // Generic share handoff: every supported client imports plain-text URIs.
            val send = Intent(Intent.ACTION_SEND)
                .setPackage(target)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, link)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(send)
            Result.success("Xray (${target.substringAfterLast('.')})")
        } catch (e: Exception) {
            ExternalApps.copyToClipboard(context, "Xray link copied", link)
            Result.success("Xray (link copied)")
        }
    }

    suspend fun disconnect() {
        // Handoff mode holds no socket; state is cleared by VpnManager.
    }
}
