package com.ironpanel.app.vpn.box

import android.net.Uri
import android.util.Base64
import org.json.JSONObject

/** Result of converting one panel config into a sing-box node. */
sealed interface BoxNode {
    data class Outbound(val json: Map<String, Any?>) : BoxNode
    data class Endpoint(val json: Map<String, Any?>) : BoxNode
}

class ConfigParseException(message: String) : Exception(message)

/** Drop null values (Gson would serialise them and sing-box rejects some). */
@Suppress("UNCHECKED_CAST")
fun clean(value: Any?): Any? = when (value) {
    is Map<*, *> -> (value as Map<String, Any?>)
        .mapNotNull { (k, v) -> clean(v)?.let { k to it } }
        .toMap()
    is List<*> -> value.mapNotNull { clean(it) }
    else -> value
}

/** Build one sing-box node from a panel Xray share link. */
fun xrayLinkToNode(link: String, tag: String): BoxNode {
    val raw = link.trim()
    val uri = try {
        Uri.parse(raw)
    } catch (_: Exception) {
        throw ConfigParseException("bad URI")
    }
    return when (uri.scheme?.lowercase()) {
        "vless" -> BoxNode.Outbound(vlessToJson(uri, tag))
        "vmess" -> BoxNode.Outbound(vmessToJson(raw, tag))
        "trojan" -> BoxNode.Outbound(trojanToJson(uri, tag))
        "ss", "shadowsocks" -> BoxNode.Outbound(ssToJson(uri, tag))
        else -> throw ConfigParseException("unsupported scheme")
    }
}

private fun qp(uri: Uri, name: String): String = uri.getQueryParameter(name) ?: ""

private fun tlsJson(security: String, sni: String, alpn: String): Map<String, Any?>? {
    if (security == "none" || security.isEmpty()) return null
    return mapOf(
        "enabled" to true,
        "server_name" to sni.ifEmpty { null },
        "alpn" to alpn.ifEmpty { null },
        "insecure" to false,
    )
}

private fun realityJson(
    security: String,
    sni: String,
    fp: String,
    pbk: String,
    sid: String,
): Map<String, Any?>? {
    if (security != "reality") return null
    return mapOf(
        "enabled" to true,
        "server_name" to sni,
        "reality" to mapOf(
            "enabled" to true,
            "public_key" to pbk,
            "short_id" to sid,
        ),
        "utls" to mapOf(
            "enabled" to true,
            "fingerprint" to fp.ifEmpty { "chrome" },
        ),
        "insecure" to false,
    )
}

private fun transportJson(network: String, uri: Uri): Map<String, Any?>? {
    return when (network.lowercase()) {
        "ws", "websocket" -> mapOf(
            "type" to "ws",
            "path" to qp(uri, "path").ifEmpty { "/" },
            "headers" to mapOf("Host" to qp(uri, "host").ifEmpty { null }),
            "max_early_data" to 2048,
            "early_data_header_name" to "Sec-WebSocket-Protocol",
        )
        "grpc", "gun" -> mapOf(
            "type" to "grpc",
            "service_name" to qp(uri, "serviceName").ifEmpty { "ironpanel-grpc" },
        )
        "tcp", "raw", "" -> null
        else -> throw ConfigParseException("transport $network not supported yet")
    }
}

private fun muxJson(uri: Uri): Map<String, Any?>? {
    if (qp(uri, "mux") !in listOf("1", "true")) return null
    return mapOf("enabled" to true, "protocol" to "smux", "max_connections" to 4)
}

private fun vlessToJson(uri: Uri, tag: String): Map<String, Any?> {
    val uuid = uri.userInfo ?: throw ConfigParseException("missing uuid")
    val host = uri.host ?: throw ConfigParseException("missing host")
    val port = if (uri.port > 0) uri.port else 443
    val network = qp(uri, "type").ifEmpty { "tcp" }
    val security = qp(uri, "security").ifEmpty { "none" }
    val sni = qp(uri, "sni")
    val tls = realityJson(security, sni, qp(uri, "fp"), qp(uri, "pbk"), qp(uri, "sid"))
        ?: tlsJson(security, sni, qp(uri, "alpn"))
    return mapOf(
        "type" to "vless",
        "tag" to tag,
        "server" to host,
        "server_port" to port,
        "uuid" to uuid,
        "flow" to qp(uri, "flow").ifEmpty { null },
        "network" to network,
        "tls" to tls,
        "transport" to transportJson(network, uri),
        "multiplex" to muxJson(uri),
    )
}

private fun vmessToJson(raw: String, tag: String): Map<String, Any?> {
    val payload = raw.substringAfter("vmess://").trim()
    val decoded = try {
        var s = payload.replace('-', '+').replace('_', '/')
        val pad = s.length % 4
        if (pad > 0) s += "=".repeat(4 - pad)
        String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
    } catch (_: Exception) {
        throw ConfigParseException("bad vmess payload")
    }
    val o = try {
        JSONObject(decoded)
    } catch (_: Exception) {
        throw ConfigParseException("bad vmess json")
    }
    val net = o.optString("net", "tcp")
    val tlsOn = o.optString("tls", "") !in listOf("", "none")
    val transport: Map<String, Any?>? = when (net.lowercase()) {
        "ws" -> mapOf(
            "type" to "ws",
            "path" to o.optString("path", "/").ifEmpty { "/" },
            "headers" to mapOf(
                "Host" to o.optString("host", "").ifEmpty { null }
            ),
        )
        "grpc" -> mapOf(
            "type" to "grpc",
            "service_name" to o.optString("path", "ironpanel-grpc").ifEmpty { "ironpanel-grpc" },
        )
        "tcp", "raw", "" -> null
        else -> throw ConfigParseException("transport $net not supported yet")
    }
    return mapOf(
        "type" to "vmess",
        "tag" to tag,
        "server" to o.optString("add").ifEmpty { throw ConfigParseException("missing host") },
        "server_port" to o.optString("port", "443").toIntOrNull()
            ?: throw ConfigParseException("bad port"),
        "uuid" to o.optString("id").ifEmpty { throw ConfigParseException("missing id") },
        "alter_id" to o.optString("aid", "0").toIntOrNull(),
        "security" to o.optString("scy", "auto").ifEmpty { "auto" },
        "tls" to if (tlsOn) {
            mapOf(
                "enabled" to true,
                "server_name" to o.optString("sni").ifEmpty { null },
                "insecure" to false,
            )
        } else null,
        "transport" to transport,
    )
}

private fun trojanToJson(uri: Uri, tag: String): Map<String, Any?> {
    val password = uri.userInfo ?: throw ConfigParseException("missing password")
    val host = uri.host ?: throw ConfigParseException("missing host")
    val port = if (uri.port > 0) uri.port else 443
    val network = qp(uri, "type").ifEmpty { "tcp" }
    val security = qp(uri, "security").ifEmpty { "tls" }
    return mapOf(
        "type" to "trojan",
        "tag" to tag,
        "server" to host,
        "server_port" to port,
        "password" to password,
        "tls" to (tlsJson(security, qp(uri, "sni"), "") ?: mapOf("enabled" to true)),
        "transport" to transportJson(network, uri),
        "multiplex" to muxJson(uri),
    )
}

private fun ssToJson(uri: Uri, tag: String): Map<String, Any?> {
    var method = ""
    var password = ""
    val host = uri.host ?: throw ConfigParseException("missing host")
    val port = if (uri.port > 0) uri.port else 8388
    val userInfo = uri.userInfo
    if (!userInfo.isNullOrEmpty() && userInfo.contains(":")) {
        method = userInfo.substringBefore(":")
        password = userInfo.substringAfter(":")
    } else {
        // ss://BASE64(method:password)@host:port
        val decoded = try {
            var s = (userInfo ?: "").replace('-', '+').replace('_', '/')
            val pad = s.length % 4
            if (pad > 0) s += "=".repeat(4 - pad)
            String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
        } catch (_: Exception) {
            throw ConfigParseException("bad ss userinfo")
        }
        if (!decoded.contains(":")) throw ConfigParseException("bad ss userinfo")
        method = decoded.substringBefore(":")
        password = decoded.substringAfter(":")
    }
    if (method.isEmpty() || password.isEmpty()) throw ConfigParseException("bad ss userinfo")
    return mapOf(
        "type" to "shadowsocks",
        "tag" to tag,
        "server" to host,
        "server_port" to port,
        "method" to method,
        "password" to password,
    )
}

/** Panel hysteria2.txt is `hy2://pass@host:port/?sni=..&insecure=0|1`. */
fun hysteriaToNode(body: String, tag: String): BoxNode {
    val line = body.lineSequence().firstOrNull { it.isNotBlank() }
        ?.trim() ?: throw ConfigParseException("empty hysteria2 config")
    val uri = try {
        Uri.parse(line)
    } catch (_: Exception) {
        throw ConfigParseException("bad hy2 URI")
    }
    if (uri.scheme?.lowercase() !in listOf("hy2", "hysteria2")) {
        throw ConfigParseException("not a hy2 URI")
    }
    val password = uri.userInfo?.ifEmpty { null }
        ?: throw ConfigParseException("missing hy2 password")
    val host = uri.host ?: throw ConfigParseException("missing host")
    val port = if (uri.port > 0) uri.port else 443
    val sni = qp(uri, "sni").ifEmpty { host }
    return BoxNode.Outbound(
        mapOf(
            "type" to "hysteria2",
            "tag" to tag,
            "server" to host,
            "server_port" to port,
            "up_mbps" to 100,
            "down_mbps" to 300,
            "password" to password,
            "tls" to mapOf(
                "enabled" to true,
                "server_name" to sni,
                "insecure" to (qp(uri, "insecure") == "1"),
            ),
        )
    )
}

/** Panel wireguard.conf → sing-box wireguard outbound. */
fun wireGuardConfToNode(conf: String, tag: String): BoxNode {
    var privateKey = ""
    var address = ""
    var dns = ""
    var mtu = 0
    var publicKey = ""
    var endpoint = ""
    var keepalive = 0
    var preshared = ""
    var section = ""
    conf.lineSequence().forEach { rawLine ->
        val line = rawLine.substringBefore("#").trim()
        if (line.startsWith("[") && line.endsWith("]")) {
            section = line.lowercase()
        } else if (line.contains("=")) {
            val key = line.substringBefore("=").trim().lowercase()
            val value = line.substringAfter("=").trim()
            when (section) {
                "[interface]" -> when (key) {
                    "privatekey" -> privateKey = value
                    "address" -> address = value.split(",").firstOrNull()?.trim() ?: ""
                    "dns" -> dns = value
                    "mtu" -> mtu = value.toIntOrNull() ?: 0
                }
                "[peer]" -> when (key) {
                    "publickey" -> publicKey = value
                    "endpoint" -> endpoint = value
                    "persistentkeepalive" -> keepalive = value.toIntOrNull() ?: 0
                    "presharedkey" -> preshared = value
                }
            }
        }
    }
    if (privateKey.isEmpty() || publicKey.isEmpty() || endpoint.isEmpty()) {
        throw ConfigParseException("incomplete wireguard.conf")
    }
    val host = endpoint.substringBeforeLast(":")
    val port = endpoint.substringAfterLast(":").toIntOrNull()
        ?: throw ConfigParseException("bad endpoint")
    if (address.isEmpty()) throw ConfigParseException("missing address")
    return BoxNode.Outbound(
        mapOf(
            "type" to "wireguard",
            "tag" to tag,
            "server" to host,
            "server_port" to port,
            "local_address" to listOf(address),
            "private_key" to privateKey,
            "peer_public_key" to publicKey,
            "pre_shared_key" to preshared.ifEmpty { null },
            "reserved" to null,
            "mtu" to if (mtu > 0) mtu else null,
            "persistent_keepalive_interval" to if (keepalive > 0) keepalive else null,
        )
    )
}

/** Panel ssh.txt (Server/Port/Username/Password lines) → sing-box ssh outbound. */
fun sshTxtToNode(body: String, tag: String): BoxNode {
    val fields = keyValueLines(body)
    val host = fields["server"]?.substringBefore(":")?.trim().orEmpty()
    val port = fields["server"]?.substringAfter(":", "")?.trim()?.toIntOrNull()
        ?: fields["port"]?.trim()?.toIntOrNull() ?: 22
    val user = fields["username"].orEmpty()
    val pass = fields["password"].orEmpty()
    if (host.isEmpty() || user.isEmpty() || pass.isEmpty() || pass == "same-as-panel") {
        throw ConfigParseException("ssh credentials incomplete")
    }
    return BoxNode.Outbound(
        mapOf(
            "type" to "ssh",
            "tag" to tag,
            "server" to host,
            "server_port" to port,
            "user" to user,
            "password" to pass,
        )
    )
}

/** Panel ocserv.txt → sing-box openconnect endpoint (AnyConnect). */
fun ocservTxtToNode(body: String, tag: String): BoxNode {
    val fields = keyValueLines(body)
    val server = fields["server"].orEmpty()
    val user = fields["username"].orEmpty()
    val pass = fields["password"].orEmpty()
    if (server.isEmpty() || user.isEmpty() || pass.isEmpty() || pass == "same-as-panel") {
        throw ConfigParseException("anyconnect credentials incomplete")
    }
    return BoxNode.Endpoint(
        mapOf(
            "type" to "openconnect",
            "tag" to tag,
            "server" to server,
            "flavor" to "anyconnect",
            "username" to user,
            "password" to pass,
        )
    )
}

/** Parse `Key: value` info files into lowercase keys. */
fun keyValueLines(body: String): Map<String, String> {
    val out = mutableMapOf<String, String>()
    body.lineSequence().forEach { line ->
        val t = line.trim()
        if (t.contains(":")) {
            out[t.substringBefore(":").trim().lowercase()] = t.substringAfter(":").trim()
        }
    }
    return out
}

/** Panel *.ovpn (cert-only, optional tls-crypt) → sing-box openvpn-client endpoint. */
fun ovpnToNode(ovpn: String, tag: String): BoxNode {
    var proto = "udp"
    var remoteHost = ""
    var remotePort = 1194
    var cipher = ""
    val dataCiphers = mutableListOf<String>()
    var auth = ""
    var authUserPass = false
    val blocks = mutableMapOf<String, StringBuilder>()
    var current: StringBuilder? = null
    var currentName = ""
    ovpn.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        when {
            line.startsWith("<") && !line.startsWith("</") && line.endsWith(">") -> {
                currentName = line.substring(1, line.length - 1)
                current = StringBuilder()
                blocks[currentName] = current!!
            }
            line.startsWith("</") -> {
                current = null
                currentName = ""
            }
            current != null -> current!!.appendLine(rawLine)
            line.startsWith("proto ") -> proto = line.removePrefix("proto ").trim()
            line.startsWith("remote ") -> {
                val parts = line.removePrefix("remote ").trim().split(Regex("\\s+"))
                if (parts.isNotEmpty()) remoteHost = parts[0]
                if (parts.size > 1) remotePort = parts[1].toIntOrNull() ?: remotePort
            }
            line.startsWith("cipher ") -> cipher = line.removePrefix("cipher ").trim()
            line.startsWith("data-ciphers ") -> dataCiphers.addAll(
                line.removePrefix("data-ciphers ").trim().split(":").map { it.trim() }
            )
            line.startsWith("auth ") && !line.startsWith("auth-") -> auth = line.removePrefix("auth ").trim()
            line == "auth-user-pass" || line.startsWith("auth-user-pass ") -> authUserPass = true
        }
    }
    if (remoteHost.isEmpty()) throw ConfigParseException("no remote in .ovpn")
    if (authUserPass) throw ConfigParseException("ovpn needs interactive credentials")
    val ca = blocks["ca"]?.toString()?.trim().orEmpty()
    val cert = blocks["cert"]?.toString()?.trim().orEmpty()
    val key = blocks["key"]?.toString()?.trim().orEmpty()
    if (ca.isEmpty() || cert.isEmpty() || key.isEmpty()) {
        throw ConfigParseException("ovpn missing inline certificates")
    }
    val network = if (proto.lowercase().startsWith("tcp")) "tcp" else "udp"
    val tlsCrypt = blocks["tls-crypt"]?.toString()?.trim()
    return BoxNode.Endpoint(
        mapOf(
            "type" to "openvpn-client",
            "tag" to tag,
            "mode" to "tls",
            "server" to remoteHost,
            "server_port" to remotePort,
            "network" to network,
            "username" to null,
            "password" to null,
            "tls" to mapOf(
                "certificate" to listOf(ca),
                "client_certificate" to listOf(cert),
                "client_key" to listOf(key),
                "control_wrap" to tlsCrypt?.let {
                    mapOf("type" to "tls_crypt", "key" to listOf(it))
                },
            ),
            "data_ciphers" to dataCiphers.ifEmpty { null },
            // NOTE: `cipher` is static-key-only in sing-box; TLS mode uses data_ciphers/auth.
            "auth" to auth.ifEmpty { null },
        )
    )
}
