package com.ironpanel.app.vpn.box

import com.google.gson.Gson
import com.ironpanel.app.data.AppSnapshot

const val PROXY_TAG = "ironapp-proxy"

private val gson = Gson()

/**
 * Build one self-contained sing-box 1.14 config for (snapshot × protocol).
 * Exactly one proxy node (outbound or endpoint) becomes the route final;
 * everything else is local plumbing. Throws ConfigParseException when the
 * panel data cannot be converted.
 *
 * Shapes follow the 1.12–1.14 migrations: type-based DNS servers,
 * sniff/hijack-dns rule actions (no legacy dns outbound), WireGuard as
 * endpoint. The app never changes the user's own config text — it only
 * translates it 1:1 into core JSON.
 */
fun buildConfig(snapshot: AppSnapshot, protocol: String, linkIndex: Int = 0): String {
    val user = snapshot.user
    val node: BoxNode = when (protocol.lowercase()) {
        "xray" -> {
            val links = user.xrayLinks
            if (links.isEmpty()) throw ConfigParseException("no Xray link for this user")
            xrayLinkToNode(links.getOrElse(linkIndex) { links.first() }, PROXY_TAG)
        }
        "wireguard" -> {
            val conf = user.configs["wireguard.conf"].orEmpty()
            if (conf.isBlank()) throw ConfigParseException("no wireguard.conf for this user")
            wireGuardConfToNode(conf, PROXY_TAG)
        }
        "hysteria2" -> {
            val body = user.configs["hysteria2.txt"].orEmpty()
            if (body.isBlank()) throw ConfigParseException("no Hysteria2 config for this user")
            hysteriaToNode(body, PROXY_TAG)
        }
        "ssh" -> {
            val body = user.configs["ssh.txt"].orEmpty()
            if (body.isBlank()) throw ConfigParseException("no SSH config for this user")
            sshTxtToNode(body, PROXY_TAG)
        }
        "openvpn" -> {
            val entry = user.configs.entries.firstOrNull { it.key.endsWith(".ovpn") }
                ?: throw ConfigParseException("no OpenVPN profile for this user")
            ovpnToNode(entry.value, PROXY_TAG)
        }
        "ocserv" -> {
            val body = user.configs["ocserv.txt"].orEmpty()
            if (body.isBlank()) throw ConfigParseException("no AnyConnect config for this user")
            ocservTxtToNode(body, PROXY_TAG)
        }
        else -> throw ConfigParseException("protocol $protocol is view-only in IronAPP")
    }
    return buildConfigFromNode(node)
}

/** Build a full config around one already-converted node (sub or custom). */
fun buildConfigFromNode(node: BoxNode): String {
    val outbounds = mutableListOf<Map<String, Any?>>()
    val endpoints = mutableListOf<Map<String, Any?>>()
    @Suppress("UNCHECKED_CAST")
    when (node) {
        is BoxNode.Outbound -> outbounds.add(node.json as Map<String, Any?>)
        is BoxNode.Endpoint -> endpoints.add(node.json as Map<String, Any?>)
    }
    outbounds.add(mapOf("type" to "direct", "tag" to "direct"))
    outbounds.add(mapOf("type" to "block", "tag" to "block"))

    val config = mapOf(
        "log" to mapOf("level" to "warning"),
        "dns" to mapOf(
            "servers" to listOf(
                mapOf("tag" to "local-dns", "type" to "local")
            ),
            "final" to "local-dns",
        ),
        "inbounds" to listOf(
            mapOf(
                "type" to "tun",
                "tag" to "tun-in",
                "address" to listOf("172.19.0.1/30"),
                "stack" to "system",
                "auto_route" to true,
                "strict_route" to false,
                "mtu" to 9000,
            )
        ),
        "outbounds" to outbounds,
        "endpoints" to endpoints.ifEmpty { null },
        "route" to mapOf(
            "rules" to listOf(
                mapOf("action" to "sniff"),
                mapOf("protocol" to "dns", "action" to "hijack-dns"),
            ),
            "final" to PROXY_TAG,
            "auto_detect_interface" to true,
        ),
    )
    @Suppress("UNCHECKED_CAST")
    return gson.toJson(clean(config) as Map<String, Any?>)
}
