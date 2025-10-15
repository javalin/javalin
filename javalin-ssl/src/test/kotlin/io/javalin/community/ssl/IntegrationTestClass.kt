package io.javalin.community.ssl

import io.javalin.Javalin
import io.javalin.community.ssl.certs.Server
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.decodeCertificatePem
import org.junit.jupiter.api.Assertions
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Function
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager

abstract class IntegrationTestClass {
    private fun assertWorks(protocol: Protocol, givenConfig: Consumer<SslConfig>) {
        var config = givenConfig
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        val url = if (protocol == Protocol.HTTP) http else https
        config = config.andThen { sslConfig: SslConfig ->
            sslConfig.insecurePort = insecurePort
            sslConfig.securePort = securePort
        }
        val app : Javalin by lazy { createTestApp(config) }
        try {
            app.start()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            Assertions.assertEquals(200, response.code)
            Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
            response.close()
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        } finally {
            app.stop()
        }
    }

    fun assertSslWorks(config: Consumer<SslConfig>) {
        assertWorks(Protocol.HTTPS, config)
    }

    fun assertHttpWorks(config: Consumer<SslConfig>) {
        assertWorks(Protocol.HTTP, config)
    }

    enum class Protocol {
        HTTP,
        HTTPS
    }

    companion object {
        @JvmField
        val log = LoggerFactory.getLogger(IntegrationTestClass::class.java)
        const val SUCCESS = "success"
        @JvmField
        val HTTPS_URL_WITH_PORT = Function { port: Int? -> String.format("https://localhost:%s/", port) }
        @JvmField
        val HTTP_URL_WITH_PORT = Function { port: Int? -> String.format("http://localhost:%s/", port) }
        private val trustAllCerts = arrayOf<X509TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        })
        @JvmStatic
        val ports = AtomicInteger(10000)
        @JvmField
        val client = createHttpsClient()
        @JvmField
        val untrustedClient = untrustedHttpsClient()
        private fun createHttpsClient(): OkHttpClient {
            val builder = HandshakeCertificates.Builder()
            builder.addTrustedCertificate(Server.CERTIFICATE_AS_STRING.decodeCertificatePem())
            try {
                val ks = KeyStore.getInstance("pkcs12")
                ks.load(Server.P12_KEY_STORE_INPUT_STREAM_SUPPLIER.get(), Server.KEY_STORE_PASSWORD.toCharArray())
                for (alias in Collections.list(ks.aliases())) {
                    builder.addTrustedCertificate(ks.getCertificate(alias) as X509Certificate)
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: CertificateException) {
                throw RuntimeException(e)
            } catch (e: KeyStoreException) {
                throw RuntimeException(e)
            }
            val clientCertificates: HandshakeCertificates = builder.build()

            return OkHttpClient.Builder()
                .hostnameVerifier { _: String?, _: SSLSession? -> true }
                .sslSocketFactory(clientCertificates.sslSocketFactory(),clientCertificates.trustManager)
                .build()
        }

        private fun untrustedHttpsClient(): OkHttpClient {
            val newBuilder: OkHttpClient.Builder = untrustedClientBuilder()
            return newBuilder.build()
        }

        @JvmStatic
        protected fun untrustedClientBuilder(): OkHttpClient.Builder {
            val sslContext: SSLContext = try {
                SSLContext.getInstance("SSL")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            try {
                sslContext.init(null, trustAllCerts, SecureRandom())
            } catch (e: KeyManagementException) {
                throw RuntimeException(e)
            }
            val newBuilder = OkHttpClient.Builder()
            newBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0])
            newBuilder.hostnameVerifier { _: String?, _: SSLSession? -> true }
            return newBuilder
        }

        @JvmStatic
        fun createTestApp(config: Consumer<SslConfig>): Javalin {
            return Javalin.create { javalinConfig: JavalinConfig ->
                javalinConfig.showJavalinBanner = false
                javalinConfig.registerPlugin(SslPlugin(config))
                javalinConfig.routes.get("/") { ctx: Context -> ctx.result(SUCCESS) }
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        protected fun testSuccessfulEndpoint(client: OkHttpClient, url: String, protocol: okhttp3.Protocol) {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            Assertions.assertEquals(200, response.code)
            Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
            Assertions.assertEquals(protocol, response.protocol)
            response.close()
        }

        @JvmStatic
        @Throws(IOException::class)
        protected fun testSuccessfulEndpoint(url: String, protocol: okhttp3.Protocol) {
            testSuccessfulEndpoint(client, url, protocol)
        }
    }
}
