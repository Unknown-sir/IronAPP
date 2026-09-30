package com.ironpanel.app.vpn

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ironpanel.app.data.AppSnapshot

/**
 * MTProto proxies live inside Telegram by design — one tap opens the
 * panel-generated tg://proxy link straight in Telegram. Nothing to download.
 */
object MtprotoConnector {
    fun open(context: Context, snapshot: AppSnapshot): Boolean {
        val link = snapshot.user.configs["telegram_proxy.txt"].orEmpty().trim()
        if (link.isEmpty()) return false
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: ActivityNotFoundException) {
            Share.copy(context, "Proxy link copied", link)
            false
        }
    }
}
