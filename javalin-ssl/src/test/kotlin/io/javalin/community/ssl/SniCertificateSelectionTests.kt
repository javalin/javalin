package io.javalin.community.ssl

import io.javalin.Javalin
import okhttp3.tls.HeldCertificate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SNIServerName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

@Tag("integration")
class SniCertificateSelectionTests : IntegrationTestClass() {

    @Test
    fun `PKIX key manager selects the certificate matching the SNI host name`() {
        val keyStore = keyStoreOf("alpha", "beta")
        withServer({ it.loadKeyStore(keyStore); it.usePkixKeyManager = true }) { port ->
            Assertions.assertEquals("alpha", servedCommonName(port, ALPHA_HOST))
            Assertions.assertEquals("beta", servedCommonName(port, BETA_HOST))
        }
    }

    @Test
    fun `PKIX key manager falls back to a certificate when no SNI or an unknown host name is sent`() {
        val keyStore = keyStoreOf("alpha", "beta")
        withServer({ it.loadKeyStore(keyStore); it.usePkixKeyManager = true }) { port ->
            Assertions.assertTrue(servedCommonName(port, null) in setOf("alpha", "beta"))
            Assertions.assertTrue(servedCommonName(port, "unknown.test") in setOf("alpha", "beta"))
        }
    }

    @Test
    fun `default key manager ignores SNI and always serves the same certificate`() {
        val keyStore = keyStoreOf("alpha", "beta")
        withServer({ it.loadKeyStore(keyStore) }) { port ->
            Assertions.assertEquals(servedCommonName(port, ALPHA_HOST), servedCommonName(port, BETA_HOST))
        }
    }

    @Test
    fun `PKIX key manager keeps selecting by SNI after a reload`() {
        val securePort = ports.getAndIncrement()
        val sslPlugin = SslPlugin {
            it.insecure = false
            it.securePort = securePort
            it.loadKeyStore(keyStoreOf("alpha", "beta"))
            it.usePkixKeyManager = true
        }
        val app = Javalin.create { config ->
            config.startup.showJavalinBanner = false
            config.registerPlugin(sslPlugin)
        }.start()
        try {
            sslPlugin.reload {
                it.loadKeyStore(keyStoreOf("alpha", "beta", suffix = "-reloaded"))
                it.usePkixKeyManager = true
            }
            Assertions.assertEquals("alpha-reloaded", servedCommonName(securePort, ALPHA_HOST))
            Assertions.assertEquals("beta-reloaded", servedCommonName(securePort, BETA_HOST))
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
        private const val ALPHA_HOST = "alpha.test"
        private const val BETA_HOST = "beta.test"

        private val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        /**
         * Creates a PKCS12 key store with one self-signed certificate per name,
         * stored under the alias `name` with the SAN `name.test` and the CN `name + suffix`.
         */
        private fun keyStoreOf(vararg names: String, suffix: String = ""): ByteArray {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(null, null)
            names.forEach { name ->
                val certificate = HeldCertificate.Builder()
                    .commonName(name + suffix)
                    .addSubjectAlternativeName("$name.test")
                    .build()
                keyStore.setKeyEntry(name, certificate.keyPair.private, PASSWORD.toCharArray(), arrayOf(certificate.certificate))
            }
            return ByteArrayOutputStream().also { keyStore.store(it, PASSWORD.toCharArray()) }.toByteArray()
        }

        private fun SslConfig.loadKeyStore(keyStore: ByteArray) =
            keystoreFromInputStream(ByteArrayInputStream(keyStore), PASSWORD)

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
                return certificate.subjectX500Principal.name.removePrefix("CN=").substringBefore(',')
            }
        }
    }
}
