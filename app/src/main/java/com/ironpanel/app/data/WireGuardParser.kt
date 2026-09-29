package com.ironpanel.app.data

/** Tiny ini-style parser for the panel's wireguard.conf (UI summary only). */
object WireGuardParser {
    data class Summary(val address: String, val endpoint: String, val dns: String)

    fun summarize(conf: String): Summary {
        var address = ""
        var endpoint = ""
        var dns = ""
        var section = ""
        conf.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.lowercase()
            } else if (line.contains("=")) {
                val key = line.substringBefore("=").trim().lowercase()
                val value = line.substringAfter("=").trim()
                when (section) {
                    "[interface]" -> when (key) {
                        "address" -> address = value
                        "dns" -> dns = value
                    }
                    "[peer]" -> if (key == "endpoint") endpoint = value
                }
            }
        }
        return Summary(address, endpoint, dns)
    }
}
