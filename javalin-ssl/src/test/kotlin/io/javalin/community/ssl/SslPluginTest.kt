package io.javalin.community.ssl

import io.javalin.Javalin
import io.javalin.community.ssl.certs.Server
import io.javalin.config.JavalinState
import io.javalin.http.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.decodeCertificatePem
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

@Tag("integration")
class SslPluginTest : IntegrationTestClass() {
    @Test
     fun `Test the reload of a pem identity`() {
        val securePort = ports.getAndIncrement()
        val https = HTTPS_URL_WITH_PORT.apply(securePort)

        // Create a http client that trusts the self-signed certificates
        val builder = HandshakeCertificates.Builder()
        builder.addTrustedCertificate(Server.CERTIFICATE_AS_STRING.decodeCertificatePem()) // Valid certificate from Vigo
        builder.addTrustedCertificate(Server.NORWAY_CERTIFICATE_AS_STRING.decodeCertificatePem()) // Valid certificate from Bergen
        val clientCertificates: HandshakeCertificates = builder.build()

        // Two clients are needed, one for the initial connection and one for after the reload, due to the way OkHttp caches connections
        val client: OkHttpClient = HostnameVerifier { _: String?, _: SSLSession? -> true }
            .let {
                OkHttpClient.Builder().sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager
                ).hostnameVerifier(it).build()
            }
        val client2: OkHttpClient = HostnameVerifier { _: String?, _: SSLSession? -> true }
            .let {
                OkHttpClient.Builder().sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager
                ).hostnameVerifier(it).build()
            }
        val sslPlugin = SslPlugin { sslConfig: SslConfig ->
            sslConfig.insecure = false
            sslConfig.securePort = securePort
            sslConfig.pemFromString(Server.NORWAY_CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
        }
        try {
            Javalin.create { config ->
                config.showJavalinBanner = false
                config.registerPlugin(sslPlugin)
                config.routes.get("/") { ctx: Context -> ctx.result(SUCCESS) }
            }.start().let { _ ->

                // Initial connection
                var res = client.newCall(Request.Builder().url(https).build()).execute()
                //Check that the certificate is the one we expect
                var cert = res.handshake!!.peerCertificates[0] as X509Certificate
                log.info("First Certificate: {}", cert.getSubjectX500Principal().name)
                Assertions.assertTrue(cert.getIssuerX500Principal().name.contains("Bergen"))

                // Reload the identity
                sslPlugin.reload { newConf: SslConfig ->
                    newConf.pemFromString(
                        Server.CERTIFICATE_AS_STRING,
                        Server.NON_ENCRYPTED_KEY_AS_STRING
                    )
                }
                // Second connection
                res = client2.newCall(Request.Builder().url(https).build()).execute()
                cert = res.handshake!!.peerCertificates[0] as X509Certificate
                log.info("Second Certificate: {}", cert.getSubjectX500Principal().name)
                Assertions.assertTrue(cert.getIssuerX500Principal().name.contains("Vigo"))
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Throws(CertificateException::class, KeyStoreException::class, IOException::class, NoSuchAlgorithmException::class)
    fun testReloadIdentityKeystore(norwayKeyStorePath: String, vigoKeyStorePath: String) {
        val securePort = ports.getAndIncrement()
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        val certificates: MutableList<X509Certificate> = ArrayList()

        // Create a http client that trusts the self-signed certificates
        val keyStore = KeyStore.getInstance(
            File(norwayKeyStorePath),
            Server.KEY_STORE_PASSWORD.toCharArray()
        ) // Valid certificate from Bergen
        keyStore.aliases().asIterator().forEachRemaining { alias: String? ->
            try {
                certificates.add(keyStore.getCertificate(alias) as X509Certificate)
            } catch (e: KeyStoreException) {
                Assertions.fail<Any>(e)
            }
        }
        val keyStore2 = KeyStore.getInstance(
            File(vigoKeyStorePath),
            Server.KEY_STORE_PASSWORD.toCharArray()
        ) // Valid certificate from Vigo
        keyStore2.aliases().asIterator().forEachRemaining { alias: String? ->
            try {
                certificates.add(keyStore2.getCertificate(alias) as X509Certificate)
            } catch (e: KeyStoreException) {
                Assertions.fail<Any>(e)
            }
        }

        // Create a http client that trusts the self-signed certificates
        val builder = HandshakeCertificates.Builder()
        for (certificate in certificates) {
            builder.addTrustedCertificate(certificate)
        }
        val clientCertificates: HandshakeCertificates = builder.build()

        // Two clients are needed, one for the initial connection and one for after the reload, due to the way OkHttp caches connections
        val client: OkHttpClient = HostnameVerifier { _: String?, _: SSLSession? -> true }
            .let {
                OkHttpClient.Builder().sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager
                ).hostnameVerifier(it).build()
            }
        val client2: OkHttpClient = HostnameVerifier { _: String?, _: SSLSession? -> true }
            .let {
                OkHttpClient.Builder().sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager
                ).hostnameVerifier(it).build()
            }
        val sslPlugin = SslPlugin { sslConfig: SslConfig ->
            sslConfig.insecure = false
            sslConfig.securePort = securePort
            sslConfig.keystoreFromPath(norwayKeyStorePath, Server.KEY_STORE_PASSWORD)
        }
        try {
            Javalin.create { config ->
                config.showJavalinBanner = false
                config.registerPlugin(sslPlugin)
                config.routes.get("/") { ctx: Context -> ctx.result(SUCCESS) }
            }.start().let { _ ->

                // Initial connection
                var res = client.newCall(Request.Builder().url(https).build()).execute()
                //Check that the certificate is the one we expect
                var cert = res.handshake!!.peerCertificates[0] as X509Certificate
                log.info("First Certificate: {}", cert.getSubjectX500Principal().name)
                Assertions.assertTrue(cert.getIssuerX500Principal().name.contains("Bergen"))

                // Reload the identity
                sslPlugin.reload { newConf: SslConfig ->
                    newConf.keystoreFromPath(
                        vigoKeyStorePath,
                        Server.KEY_STORE_PASSWORD
                    )
                }

                // Second connection
                res = client2.newCall(Request.Builder().url(https).build()).execute()
                cert = res.handshake!!.peerCertificates[0] as X509Certificate
                log.info("Second Certificate: {}", cert.getSubjectX500Principal().name)
                Assertions.assertTrue(cert.getIssuerX500Principal().name.contains("Vigo"))
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test the reload of a p12 identity`() {
        try {
            testReloadIdentityKeystore(Server.NORWAY_P12_KEY_STORE_PATH, Server.P12_KEY_STORE_PATH)
        } catch (e: Exception) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test the reload of JKS identity`() {
        try {
            testReloadIdentityKeystore(Server.NORWAY_JKS_KEY_STORE_PATH, Server.JKS_KEY_STORE_PATH)
        } catch (e: Exception) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the reload of a server with no SSL connector fails`() {
        val insecurePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val sslPlugin = SslPlugin { sslConfig: SslConfig ->
            sslConfig.secure = false
            sslConfig.insecurePort = insecurePort
        }
        try {
            Javalin.create { config ->
                config.showJavalinBanner = false
                config.registerPlugin(sslPlugin)
                config.routes.get("/") { ctx: Context -> ctx.result(SUCCESS) }
            }.start().let { _ ->
                val res = OkHttpClient().newCall(Request.Builder().url(http).build()).execute()
                Assertions.assertTrue(res.isSuccessful)
                Assertions.assertThrows(IllegalStateException::class.java) {
                    sslPlugin.reload { newConf: SslConfig ->
                        newConf.pemFromString(
                            Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING
                        )
                    }
                }
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the reload of a non started server fails`() {
        val sslPlugin = SslPlugin { sslConfig: SslConfig ->
            sslConfig.secure = false
            sslConfig.insecurePort = ports.getAndIncrement()
        }
        Assertions.assertThrows(IllegalStateException::class.java) {
            sslPlugin.reload { newConf: SslConfig ->
                newConf.pemFromString(
                    Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING
                )
            }
        }
    }
}
