package com.ironpanel.app.vpn.box

import android.util.Base64
import com.ironpanel.libbox.NetworkInterfaceIterator
import com.ironpanel.libbox.StringIterator
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** gomobile StringIterator backed by a Kotlin list. */
class StringArray(source: Iterator<String>) : StringIterator {
    private val items = source.asSequence().toList()
    private var index = 0
    override fun len(): Int = items.size
    override fun hasNext(): Boolean = index < items.size
    override fun next(): String = if (index < items.size) items[index++] else ""
}

/** gomobile NetworkInterfaceIterator backed by a Kotlin list. */
class InterfaceArray(
    private val items: List<com.ironpanel.libbox.NetworkInterface>,
) : NetworkInterfaceIterator {
    private var index = 0
    override fun len(): Int = items.size
    override fun hasNext(): Boolean = index < items.size
    override fun next(): com.ironpanel.libbox.NetworkInterface = items[index++]
}

/** Android system CA certificates in PEM form for the core's TLS verifier. */
fun systemCaPemList(): List<String> {
    val out = mutableListOf<String>()
    try {
        val ks = java.security.KeyStore.getInstance("AndroidCAStore")
        ks.load(null, null)
        val factory = CertificateFactory.getInstance("X.509")
        val aliases = ks.aliases()
        while (aliases.hasMoreElements()) {
            try {
                val cert = ks.getCertificate(aliases.nextElement()) as? X509Certificate ?: continue
                val pem = "-----BEGIN CERTIFICATE-----\n" +
                    Base64.encodeToString(cert.encoded, Base64.NO_WRAP) +
                    "\n-----END CERTIFICATE-----\n"
                factory.generateCertificate(ByteArrayInputStream(pem.toByteArray()))
                out.add(pem)
            } catch (_: Exception) {
            }
        }
    } catch (_: Exception) {
    }
    return out
}
