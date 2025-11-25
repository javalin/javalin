package io.javalin.community.ssl.certs

import io.javalin.Javalin
import io.javalin.community.ssl.IntegrationTestClass
import io.javalin.community.ssl.SslConfig
import io.javalin.community.ssl.SslPlugin
import io.javalin.community.ssl.TrustConfig
import io.javalin.config.JavalinState
import io.javalin.http.Context
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.pem.util.PemUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.server.ServerConnector
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.*
import java.util.function.Supplier

/**
 * Tests for the testing the trust of Certificates using a CA.
 * [issue](https://github.com/javalin/javalin-ssl/issues/56#issuecomment-1378373123)
 */
@Tag("integration")
class CertificateAuthorityTests : IntegrationTestClass() {
    @Test
    fun `Client certificate works when trusting root CA`() {
        val keyManager = PemUtils.loadIdentityMaterial(CLIENT_FULLCHAIN_CER, CLIENT_KEY_NAME)
        val sslFactory = SSLFactory.builder()
            .withIdentityMaterial(keyManager)
            .withTrustingAllCertificatesWithoutValidation()
            .build()
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.orElseThrow())
        builder.hostnameVerifier(sslFactory.hostnameVerifier)
        assertClientWorks(builder.build())
    }

    @Test
    fun `Client fails when no certificate is provided`() {
        val sslFactory = SSLFactory.builder()
            .withTrustingAllCertificatesWithoutValidation()
            .build()
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.orElseThrow())
        builder.hostnameVerifier(sslFactory.hostnameVerifier)
        assertClientFails(builder.build())
    }

    @Test
    fun `Client fails when a self-signed certificate is provided, and a CA is trusted`() {
        val sslFactory = SSLFactory.builder()
            .withIdentityMaterial(
                PemUtils.parseIdentityMaterial(
                    Client.CLIENT_CERTIFICATE_AS_STRING,
                    Client.CLIENT_PRIVATE_KEY_AS_STRING,
                    "".toCharArray()
                )
            )
            .withTrustingAllCertificatesWithoutValidation()
            .build()
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.orElseThrow())
        builder.hostnameVerifier(sslFactory.hostnameVerifier)
        assertClientFails(builder.build())
    }

    @Test
    fun `Client fails when a certificate without chain is provided, and a CA is trusted`() {
        val keyManager = PemUtils.loadIdentityMaterial(CLIENT_CER, CLIENT_KEY_NAME)
        val sslFactory = SSLFactory.builder()
            .withIdentityMaterial(keyManager)
            .withTrustingAllCertificatesWithoutValidation()
            .build()
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.orElseThrow())
        builder.hostnameVerifier(sslFactory.hostnameVerifier)
        assertClientFails(builder.build())
    }

    @Test
    fun `mTLS works when trusting a root CA, and an intermediate CA issues both the client and server certificates`() {
        val keyManager = PemUtils.loadIdentityMaterial(CLIENT_FULLCHAIN_CER, CLIENT_KEY_NAME)
        val sslFactory = SSLFactory.builder()
            .withIdentityMaterial(keyManager)
            .withTrustMaterial(PemUtils.loadTrustMaterial(ROOT_CERT_NAME))
            .withUnsafeHostnameVerifier() // we don't care about the hostname, we just want to test the certificate
            .build()
        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.orElseThrow())
        builder.hostnameVerifier(sslFactory.hostnameVerifier)
        assertClientWorks(builder.build())
    }

    @Test
    fun `Hot reloading works when using mTLS`() {
        val keyManager = PemUtils.loadIdentityMaterial(CLIENT_FULLCHAIN_CER, CLIENT_KEY_NAME)
        val client = Supplier<OkHttpClient> {
            val sslFactory = SSLFactory.builder()
                .withIdentityMaterial(keyManager)
                .withTrustMaterial(PemUtils.loadTrustMaterial(ROOT_CERT_NAME)) // root cert of the client above
                .withUnsafeHostnameVerifier() // we don't care about the hostname, we just want to test the certificate
                .build()
            OkHttpClient.Builder()
                .sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.orElseThrow())
                .hostnameVerifier(sslFactory.hostnameVerifier)
                .build()
        }
        val securePort = ports.getAndIncrement()
        val url = HTTPS_URL_WITH_PORT.apply(securePort)
        val sslPlugin = SslPlugin { config: SslConfig ->
            config.insecure = false
            config.securePort = securePort
            config.pemFromClasspath(SERVER_CERT_NAME, SERVER_KEY_NAME)
            config.http2 = false
            config.configConnectors { conn: ServerConnector -> conn.idleTimeout = 0 } // disable idle timeout for testing
            config.withTrustConfig { trustConfig: TrustConfig -> trustConfig.certificateFromClasspath(ROOT_CERT_NAME) }
        }
        try {
            Javalin.create { config  ->
                config.showJavalinBanner = false
                config.registerPlugin(sslPlugin)
                config.routes.get("/") { ctx: Context -> ctx.result(SUCCESS) }
            }.start().let { _ ->
                testSuccessfulEndpoint(url, client.get()) // works
                sslPlugin.reload { config: SslConfig ->
                    config.pemFromClasspath(SERVER_CERT_NAME, SERVER_KEY_NAME)
                    config.withTrustConfig { trustConfig: TrustConfig ->
                        trustConfig.certificateFromClasspath(Server.CERTIFICATE_FILE_NAME) // this is some other certificate
                    }
                }
                testWrongCertOnEndpoint(
                    url,
                    client.get()
                ) // fails because the server now has a different trust material
                sslPlugin.reload { config: SslConfig ->
                    config.pemFromClasspath(SERVER_CERT_NAME, SERVER_KEY_NAME)
                    config.withTrustConfig { trustConfig: TrustConfig ->
                        trustConfig.certificateFromClasspath(ROOT_CERT_NAME) // back to the original certificate
                    }
                }
                testSuccessfulEndpoint(url, client.get()) // works again
            }
        } catch (e: Exception) {
            Assertions.fail<Any>(e)
        }
    }

    companion object {
        const val ROOT_CERT_NAME = "ca/root-ca.cer"
        const val CLIENT_FULLCHAIN_CER = "ca/client-fullchain.cer"
        const val CLIENT_CER = "ca/client-nochain.cer"
        const val CLIENT_KEY_NAME = "ca/client.key"
        const val SERVER_CERT_NAME = "ca/server.cer"
        const val SERVER_KEY_NAME = "ca/server.key"

        @Throws(IOException::class)
        protected fun testSuccessfulEndpoint(url: String, client: OkHttpClient) {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            Assertions.assertEquals(200, response.code)
            Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
            response.close()
            client.connectionPool.evictAll()
        }

        protected fun testWrongCertOnEndpoint(url: String, client: OkHttpClient) {
            Assertions.assertThrows(Exception::class.java) {
                client.newCall(Request.Builder().url(url).build()).execute().close()
                client.connectionPool.evictAll()
            }
        }

        protected fun assertClientWorks(client: OkHttpClient) {
            val securePort = ports.getAndIncrement()
            val url = HTTPS_URL_WITH_PORT.apply(securePort)
            try {
                createTestApp { config: SslConfig ->
                    config.insecure = false
                    config.securePort = securePort
                    config.pemFromClasspath(SERVER_CERT_NAME, SERVER_KEY_NAME)
                    config.http2 = false
                    config.withTrustConfig { trustConfig: TrustConfig ->
                        trustConfig.certificateFromClasspath(
                            ROOT_CERT_NAME
                        )
                    }
                }.start().let { _ -> testSuccessfulEndpoint(url, client) }
            } catch (e: Exception) {
                Assertions.fail<Any>(e)
            }
        }

        protected fun assertClientFails(client: OkHttpClient) {
            val securePort = ports.getAndIncrement()
            val url = HTTPS_URL_WITH_PORT.apply(securePort)
            try {
                createTestApp { config: SslConfig ->
                    config.insecure = false
                    config.securePort = securePort
                    config.pemFromClasspath(SERVER_CERT_NAME, SERVER_KEY_NAME)
                    config.http2 = false
                    config.withTrustConfig { trustConfig: TrustConfig ->
                        trustConfig.certificateFromClasspath(
                            ROOT_CERT_NAME
                        )
                    }
                }.start().let { _ -> testWrongCertOnEndpoint(url, client) }
            } catch (e: Exception) {
                Assertions.fail<Any>(e)
            }
        }
    }
}
