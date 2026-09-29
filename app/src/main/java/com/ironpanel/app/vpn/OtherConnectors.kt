package com.ironpanel.app.vpn

import android.content.Context
import android.content.Intent
import com.ironpanel.app.data.AppSnapshot

/**
 * Hysteria2: hands the panel-generated URI to a compatible client
 * (Hiddify/NekoBox import plain-text URIs), clipboard as fallback.
 */
object HysteriaConnector {
    suspend fun connect(context: Context, snapshot: AppSnapshot): Result<String> {
        val body = snapshot.user.configs["hysteria2.txt"].orEmpty().trim()
        if (body.isEmpty()) return Result.failure(IllegalStateException("No Hysteria2 config"))
        val firstLine = body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val target = listOf(ExternalApps.HIDDIFY, ExternalApps.NEKOBOX, ExternalApps.V2RAY_NG)
            .firstOrNull { ExternalApps.installed(context, it) }
        if (target == null) {
            ExternalApps.copyToClipboard(context, "Hysteria2 config copied", body)
            ExternalApps.openPlayStore(context, ExternalApps.HIDDIFY)
            return Result.success("Hysteria2 (link copied)")
        }
        return try {
            context.startActivity(
                Intent(Intent.ACTION_SEND).setPackage(target).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, firstLine)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Result.success("Hysteria2 (${target.substringAfterLast('.')})")
        } catch (e: Exception) {
            ExternalApps.copyToClipboard(context, "Hysteria2 config copied", body)
            Result.success("Hysteria2 (link copied)")
        }
    }

    suspend fun disconnect() = Unit
}

/**
 * Generic protocols (ocserv/AnyConnect, l2tp, pptp, ssh, telegram_proxy):
 * copy prefilled credentials + deep-link the system client. Modern Android
 * (12+) removed built-in L2TP/PPTP, so handoff is the only reliable path.
 */
object GenericConnector {
    suspend fun connect(context: Context, snapshot: AppSnapshot, protocol: String): Result<String> {
        val body = configFor(snapshot, protocol)
        if (body.isBlank()) return Result.failure(IllegalStateException("No config for $protocol"))
        when (protocol.lowercase()) {
            "telegram_proxy" -> {
                if (ExternalApps.installed(context, ExternalApps.TELEGRAM)) {
                    ExternalApps.copyToClipboard(context, "Proxy copied — paste in Telegram", body)
                    return Result.success("MTProto (copied)")
                }
            }
        }
        ExternalApps.copyToClipboard(context, "$protocol config copied", body)
        return Result.success("$protocol (copied)")
    }

    private fun configFor(snapshot: AppSnapshot, protocol: String): String {
        val configs = snapshot.user.configs
        return when (protocol.lowercase()) {
            "ocserv" -> configs["ocserv.txt"].orEmpty()
            "l2tp" -> configs["l2tp.txt"].orEmpty()
            "pptp" -> configs["pptp.txt"].orEmpty()
            "ssh" -> configs["ssh.txt"].orEmpty()
            "telegram_proxy" -> configs["telegram_proxy.txt"].orEmpty()
            else -> ""
        }
    }
}
