package io.javalin.community.ssl.util

import org.eclipse.jetty.util.ssl.X509
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.ExtendedSSLSession
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedKeyManager

/**
 * Key manager that selects the server certificate matching the SNI host name sent by the client,
 * following the same rules as Jetty's SNI support. When the client doesn't send a SNI host name,
 * or no certificate matches it, the selection is delegated to the wrapped key manager.
 *
 * @param delegate The key manager to wrap.
 * @param keyStore The key store the delegate was created from.
 */
class SniKeyManager(private val delegate: X509ExtendedKeyManager, keyStore: KeyStore) : X509ExtendedKeyManager() {

    private val certificates: List<X509> = keyStore.aliases().toList()
        .filter(keyStore::isKeyEntry)
        .mapNotNull { alias -> (keyStore.getCertificate(alias) as? X509Certificate)?.let { X509(alias, it) } }

    override fun chooseEngineServerAlias(keyType: String?, issuers: Array<Principal>?, engine: SSLEngine?): String? =
        chooseServerAlias(keyType, issuers, engine?.handshakeSession) { delegate.chooseEngineServerAlias(keyType, issuers, engine) }

    override fun chooseServerAlias(keyType: String?, issuers: Array<Principal>?, socket: Socket?): String? =
        chooseServerAlias(keyType, issuers, (socket as? SSLSocket)?.handshakeSession) { delegate.chooseServerAlias(keyType, issuers, socket) }

    private fun chooseServerAlias(keyType: String?, issuers: Array<Principal>?, session: SSLSession?, fallback: () -> String?): String? {
        val host = (session as? ExtendedSSLSession)?.requestedServerNames
            ?.filterIsInstance<SNIHostName>()
            ?.firstOrNull()
            ?.asciiName
            ?: return fallback()

        val matching = delegate.getServerAliases(keyType, issuers).orEmpty()
            .mapNotNull { alias -> delegate.getCertificateChain(alias)?.firstOrNull()?.let { X509(alias, it) } }
            .filter { it.matches(host) }

        return when {
            // Prefer exact matches over wildcard matches
            matching.isNotEmpty() -> matching.minBy { it.wilds.size }.alias
            // A certificate with another key type matches, so this key type must not be used
            certificates.any { it.matches(host) } -> null
            else -> fallback()
        }
    }

    override fun getServerAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? = delegate.getServerAliases(keyType, issuers)
    override fun getClientAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? = delegate.getClientAliases(keyType, issuers)
    override fun chooseClientAlias(keyType: Array<String>?, issuers: Array<Principal>?, socket: Socket?): String? = delegate.chooseClientAlias(keyType, issuers, socket)
    override fun chooseEngineClientAlias(keyType: Array<String>?, issuers: Array<Principal>?, engine: SSLEngine?): String? = delegate.chooseEngineClientAlias(keyType, issuers, engine)
    override fun getCertificateChain(alias: String?): Array<X509Certificate>? = delegate.getCertificateChain(alias)
    override fun getPrivateKey(alias: String?): PrivateKey? = delegate.getPrivateKey(alias)
}
