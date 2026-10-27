package io.javalin.community.ssl

import io.javalin.Javalin
import nl.altindag.ssl.pem.util.PemUtils
import nl.altindag.ssl.util.KeyManagerUtils
import nl.altindag.ssl.util.KeyStoreUtils
import okhttp3.Dns
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.nio.file.Path
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SNIServerName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

/**
 * Serving multiple certificates selected by SNI host name, using a custom key manager built with ayza:
 * one identity per certificate, and an identity route per host name.
 * The fixtures contain an RSA certificate for alpha.test (alias alpha) and an EC certificate for beta.test (alias beta).
 */
@Tag("integration")
class SniCertificateSelectionTests : IntegrationTestClass() {

    @Test
    fun `a key store loaded directly serves the same certificate for every SNI host name`() {
        withServer({ it.keystoreFromClasspath(P12_FILE, PASSWORD) }) { port ->
            Assertions.assertEquals(servedCommonName(port, "alpha.test"), servedCommonName(port, "beta.test"))
        }
    }

    @Test
    fun `certificates are selected by SNI host name from p12 and jks files`() {
        for (file in listOf(P12_FILE, JKS_FILE)) {
            for (tlsConfig in listOf(TlsConfig.INTERMEDIATE, TlsConfig.MODERN)) {
                val keyStore = KeyStoreUtils.loadKeyStore(file, PASSWORD.toCharArray())
                withServer({ it.keyManager(sniKeyManager(keyStore.split("alpha"), keyStore.split("beta"))); it.tlsConfig = tlsConfig }) { port ->
                    Assertions.assertEquals("alpha", servedCommonName(port, "alpha.test"))
                    Assertions.assertEquals("beta", servedCommonName(port, "beta.test"))
                }
            }
        }
    }

    @Test
    fun `certificates are selected by SNI host name from pem files`() {
        withServer({ it.keyManager(sniKeyManager(pemIdentity("alpha"), pemIdentity("beta"))) }) { port ->
            Assertions.assertEquals("alpha", servedCommonName(port, "alpha.test"))
            Assertions.assertEquals("beta", servedCommonName(port, "beta.test"))
        }
    }

    @Test
    fun `a certificate is still served when no SNI or an unknown host name is sent`() {
        withServer({ it.keyManager(sniKeyManager(pemIdentity("alpha"), pemIdentity("beta"))) }) { port ->
            Assertions.assertTrue(servedCommonName(port, null) in HOSTS.keys)
            Assertions.assertTrue(servedCommonName(port, "unknown.test") in HOSTS.keys)
        }
    }

    @Test
    fun `https requests pass the SNI host check for every certificate`() {
        val client = untrustedClientBuilder()
            .dns(object : Dns { override fun lookup(hostname: String) = listOf(InetAddress.getLoopbackAddress()) })
            .build()
        withServer({ it.keyManager(sniKeyManager(pemIdentity("alpha"), pemIdentity("beta"))); it.sniHostCheck = true }) { port ->
            for (host in listOf("alpha.test", "beta.test")) {
                testSuccessfulEndpoint(client, "https://$host:$port/", okhttp3.Protocol.HTTP_2)
            }
        }
    }

    @Test
    fun `certificates are selected by SNI host name after reloading a key store into a key manager`() {
        val securePort = ports.getAndIncrement()
        val sslPlugin = SslPlugin {
            it.insecure = false
            it.securePort = securePort
            it.keystoreFromClasspath(P12_FILE, PASSWORD)
        }
        val app = Javalin.create { config ->
            config.startup.showJavalinBanner = false
            config.registerPlugin(sslPlugin)
        }.start()
        try {
            sslPlugin.reload { it.keyManager(sniKeyManager(pemIdentity("alpha"), pemIdentity("beta"))) }
            Assertions.assertEquals("alpha", servedCommonName(securePort, "alpha.test"))
            Assertions.assertEquals("beta", servedCommonName(securePort, "beta.test"))
        } finally {
            app.stop()
        }
    }

    private fun withServer(config: (SslConfig) -> Unit, test: (Int) -> Unit) {
        val securePort = ports.getAndIncrement()
        val app = createTestApp {
            it.insecure = false
            it.securePort = securePort
            config(it)
        }.start()
        try {
            test(securePort)
        } finally {
            app.stop()
        }
    }

    companion object {
        private const val PASSWORD = "password"
        private const val P12_FILE = "server/sni/sni.p12"
        private const val JKS_FILE = "server/sni/sni.jks"
        private val HOSTS = mapOf("alpha" to "alpha.test", "beta" to "beta.test")

        /** The recipe from the docs: one key manager per identity, combined, with an identity route per host name. */
        private fun sniKeyManager(vararg identities: KeyStore): X509ExtendedKeyManager {
            val keyManager = KeyManagerUtils.combine(identities.map { KeyManagerUtils.createKeyManager(it, PASSWORD.toCharArray()) })
            identities.flatMap { it.aliases().toList() }.forEach { alias ->
                KeyManagerUtils.addIdentityRoute(keyManager, alias, "https://${HOSTS.getValue(alias)}:443")
            }
            return keyManager
        }

        /** Copies the given alias of a key store holding multiple certificates into its own key store. */
        private fun KeyStore.split(alias: String): KeyStore =
            KeyStoreUtils.createIdentityStore(getKey(alias, PASSWORD.toCharArray()), PASSWORD.toCharArray(), alias, *getCertificateChain(alias))

        private fun pemIdentity(alias: String): KeyStore =
            KeyStoreUtils.createIdentityStore(
                PemUtils.loadPrivateKey(resource("server/sni/$alias.key")),
                PASSWORD.toCharArray(),
                alias,
                PemUtils.loadCertificate(resource("server/sni/$alias.crt"))
            )

        private fun resource(name: String): Path = Path.of(ClassLoader.getSystemResource(name).toURI())

        private val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        /**
         * Performs a TLS handshake sending the given SNI host name (none if null)
         * and returns the CN of the certificate served by the server.
         */
        private fun servedCommonName(port: Int, sniHostName: String?): String {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(trustAll), SecureRandom())
            (sslContext.socketFactory.createSocket("localhost", port) as SSLSocket).use { socket ->
                val parameters = socket.sslParameters
                parameters.serverNames = sniHostName?.let { listOf<SNIServerName>(SNIHostName(it)) } ?: emptyList()
                socket.sslParameters = parameters
                socket.startHandshake()
                val certificate = socket.session.peerCertificates[0] as X509Certificate
                return Regex("CN=([^,]+)").find(certificate.subjectX500Principal.name)!!.groupValues[1]
            }
        }
    }
}
