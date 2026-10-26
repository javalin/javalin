package io.javalin.community.ssl

import io.javalin.Javalin
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HeldCertificate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
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
    fun `the certificate matching the SNI host name is selected`() {
        val keyStore = keyStoreOf(Cert("alpha", "alpha.test"), Cert("beta", "beta.test"))
        withServer(keyStore, sni = true) { port ->
            Assertions.assertEquals("alpha", servedCommonName(port, "alpha.test"))
            Assertions.assertEquals("beta", servedCommonName(port, "beta.test"))
        }
    }

    @Test
    fun `the certificate matching the SNI host name is selected with TLS 1_3`() {
        val keyStore = keyStoreOf(Cert("alpha", "alpha.test"), Cert("beta", "beta.test"))
        withServer(keyStore, sni = true, { it.tlsConfig = TlsConfig.MODERN }) { port ->
            Assertions.assertEquals("alpha", servedCommonName(port, "alpha.test"))
            Assertions.assertEquals("beta", servedCommonName(port, "beta.test"))
        }
    }

    @Test
    fun `the certificate matching the SNI host name is selected across key types`() {
        val keyStore = keyStoreOf(Cert("alpha", "alpha.test", rsa = true), Cert("beta", "beta.test"))
        for (tlsConfig in listOf(TlsConfig.INTERMEDIATE, TlsConfig.MODERN)) {
            withServer(keyStore, sni = true, { it.tlsConfig = tlsConfig }) { port ->
                Assertions.assertEquals("alpha", servedCommonName(port, "alpha.test"))
                Assertions.assertEquals("beta", servedCommonName(port, "beta.test"))
            }
        }
    }

    @Test
    fun `exact matches are preferred over wildcard matches`() {
        val keyStore = keyStoreOf(Cert("wildcard", "*.example.test"), Cert("exact", "exact.example.test"))
        withServer(keyStore, sni = true) { port ->
            Assertions.assertEquals("exact", servedCommonName(port, "exact.example.test"))
            Assertions.assertEquals("wildcard", servedCommonName(port, "other.example.test"))
        }
    }

    @Test
    fun `the default certificate is served when no SNI or an unknown host name is sent`() {
        val keyStore = keyStoreOf(Cert("alpha", "alpha.test"), Cert("beta", "beta.test"))
        val defaultCertificate = withServer(keyStore, sni = false) { port -> servedCommonName(port, null) }
        withServer(keyStore, sni = true) { port ->
            Assertions.assertEquals(defaultCertificate, servedCommonName(port, null))
            Assertions.assertEquals(defaultCertificate, servedCommonName(port, "unknown.test"))
        }
    }

    @Test
    fun `SNI is ignored by default`() {
        val keyStore = keyStoreOf(Cert("alpha", "alpha.test"), Cert("beta", "beta.test"))
        withServer(keyStore, sni = false) { port ->
            Assertions.assertEquals(servedCommonName(port, "alpha.test"), servedCommonName(port, "beta.test"))
        }
    }

    @Test
    fun `https requests pass the SNI host check for every certificate`() {
        val keyStore = keyStoreOf(Cert("alpha", "alpha.test"), Cert("beta", "beta.test"))
        val client = untrustedClientBuilder()
            .dns(object : Dns { override fun lookup(hostname: String) = listOf(InetAddress.getLoopbackAddress()) })
            .build()
        withServer(keyStore, sni = true, { it.sniHostCheck = true }) { port ->
            for (host in listOf("alpha.test", "beta.test")) {
                testSuccessfulEndpoint(client, "https://$host:$port/", okhttp3.Protocol.HTTP_2)
            }
        }
    }

    @Test
    fun `the certificate matching the SNI host name is selected after a reload`() {
        val securePort = ports.getAndIncrement()
        val sslPlugin = SslPlugin {
            it.insecure = false
            it.securePort = securePort
            it.loadKeyStore(keyStoreOf(Cert("alpha", "alpha.test"), Cert("beta", "beta.test")))
            it.useSniKeyManager = true
        }
        val app = Javalin.create { config ->
            config.startup.showJavalinBanner = false
            config.registerPlugin(sslPlugin)
        }.start()
        try {
            sslPlugin.reload {
                it.loadKeyStore(keyStoreOf(Cert("alpha-reloaded", "alpha.test"), Cert("beta-reloaded", "beta.test")))
                it.useSniKeyManager = true
            }
            Assertions.assertEquals("alpha-reloaded", servedCommonName(securePort, "alpha.test"))
            Assertions.assertEquals("beta-reloaded", servedCommonName(securePort, "beta.test"))
        } finally {
            app.stop()
        }
    }

    private fun <T> withServer(keyStore: ByteArray, sni: Boolean, config: (SslConfig) -> Unit = {}, test: (Int) -> T): T {
        val securePort = ports.getAndIncrement()
        val app = createTestApp {
            it.insecure = false
            it.securePort = securePort
            it.loadKeyStore(keyStore)
            it.useSniKeyManager = sni
            config(it)
        }.start()
        try {
            return test(securePort)
        } finally {
            app.stop()
        }
    }

    private data class Cert(val commonName: String, val hostName: String, val rsa: Boolean = false)

    companion object {
        private const val PASSWORD = "password"

        private val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        /**
         * Creates a PKCS12 key store with one self-signed certificate per given [Cert],
         * stored under its common name as alias.
         */
        private fun keyStoreOf(vararg certs: Cert): ByteArray {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(null, null)
            certs.forEach { cert ->
                val builder = HeldCertificate.Builder()
                    .commonName(cert.commonName)
                    .addSubjectAlternativeName(cert.hostName)
                if (cert.rsa) builder.rsa2048()
                val certificate = builder.build()
                keyStore.setKeyEntry(cert.commonName, certificate.keyPair.private, PASSWORD.toCharArray(), arrayOf(certificate.certificate))
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
