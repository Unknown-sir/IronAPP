package com.ironpanel.app.data

import android.net.Uri

/** Minimal parser for vless://, vmess://, trojan:// and ss:// share links. */
object XrayLinkParser {
    data class Parsed(
        val scheme: String,
        val remark: String,
        val host: String,
        val port: Int,
        val params: Map<String, String>,
    )

    fun parse(link: String): Parsed? {
        val raw = link.trim()
        if (raw.isEmpty()) return null
        return try {
            when {
                raw.startsWith("vless://", true) ||
                    raw.startsWith("trojan://", true) ||
                    raw.startsWith("ss://", true) -> {
                    val uri = Uri.parse(raw)
                    val fragment = uri.fragment ?: ""
                    val params = mutableMapOf<String, String>()
                    uri.queryParameterNames.forEach { name ->
                        params[name] = uri.getQueryParameter(name) ?: ""
                    }
                    Parsed(
                        scheme = (uri.scheme ?: "").lowercase(),
                        remark = fragment.ifEmpty { uri.getQueryParameter("remarks") ?: "" },
                        host = uri.host ?: "",
                        port = if (uri.port > 0) uri.port else 443,
                        params = params,
                    )
                }
                raw.startsWith("vmess://", true) -> {
                    // vmess payload is base64 JSON; keep the remark only.
                    Parsed("vmess", "", "", 0, emptyMap())
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun labelFor(link: String, index: Int): String {
        val parsed = parse(link)
        val remark = parsed?.remark?.takeIf { it.isNotBlank() }
        val host = parsed?.host?.takeIf { it.isNotBlank() }
        return when {
            remark != null -> remark
            host != null -> "${parsed.scheme.uppercase()} · $host"
            else -> "Xray ${index + 1}"
        }
    }
}
