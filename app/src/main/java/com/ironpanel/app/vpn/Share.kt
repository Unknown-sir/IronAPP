package com.ironpanel.app.vpn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/** Clipboard helper (no external VPN apps are used by IronAPP anymore). */
object Share {
    fun copy(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
    }
}
