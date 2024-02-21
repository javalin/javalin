package io.javalin.community.ssl

import io.javalin.Javalin
import io.javalin.community.ssl.certs.Server
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jetty.server.ConnectionFactory
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.*

@Tag("integration")
class SslConfigTests : IntegrationTestClass() {
    @Test
    fun `Test that the insecure connector is disabled when insecure is set to false`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecure = false
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.securePort = securePort
                config.insecurePort = insecurePort
            }.start().let { app ->
                Assertions.assertThrows(Exception::class.java) {
                    client.newCall(Request.Builder().url(http).build()).execute()
                } // should throw exception
                val response = client.newCall(Request.Builder().url(https).build()).execute() // should not throw exception
                Assertions.assertEquals(200, response.code)
                Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
                app.stop()
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
    fun `Test that the secure connector is disabled when insecure is set to true`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.secure = false
                config.insecurePort = insecurePort
                config.securePort = securePort
            }.start().let { _ ->
                Assertions.assertThrows(Exception::class.java) {
                    client.newCall(Request.Builder().url(https).build()).execute()
                } // should throw exception
                val response = client.newCall(Request.Builder().url(http).build()).execute() // should not throw exception
                Assertions.assertEquals(200, response.code)
                Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
    fun `Test that the insecure port can be changed`() {
        try {
            createTestApp { config: SslConfig ->
                config.secure = false
                config.insecurePort = 8080
            }.start().let { _ ->
                val response = client.newCall(Request.Builder().url("http://localhost:8080/").build())
                    .execute() // should not throw exception
                Assertions.assertEquals(200, response.code)
                Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
    fun `Test that the secure port can be changed`() {
        try {
            createTestApp { config: SslConfig ->
                config.insecure = false
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.securePort = 8443
            }.start().let { _ ->
                val response = client.newCall(Request.Builder().url("https://localhost:8443/").build())
                    .execute() // should not throw exception
                Assertions.assertEquals(200, response.code)
                Assertions.assertEquals(SUCCESS, Objects.requireNonNull(response.body)?.string())
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that redirecting from http to https works`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        val noRedirectClient: OkHttpClient = untrustedClientBuilder().also { it.followSslRedirects(false) }.build()
        try {
            createTestApp { config: SslConfig ->
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.securePort = securePort
                config.insecurePort = insecurePort
                config.redirect = true
            }.start().let { _ ->
                val redirect = noRedirectClient.newCall(Request.Builder().url(http).build()).execute()
                Assertions.assertTrue(redirect.isRedirect)
                Assertions.assertEquals(https, redirect.header("Location"))
                val redirected = client.newCall(Request.Builder().url(http).build()).execute()
                Assertions.assertEquals(200, redirected.code)
                Assertions.assertEquals(SUCCESS, redirected.body!!.string())
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the insecure connector works with http1-1`() {
        val insecurePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        try {
            createTestApp { config: SslConfig ->
                config.secure = false
                config.http2 = false
                config.insecurePort = insecurePort
            }.start().let { _ -> testSuccessfulEndpoint(http, okhttp3.Protocol.HTTP_1_1) }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that http2 can be disabled on the insecure connector`() {
        val http2client: OkHttpClient =
            listOf(okhttp3.Protocol.H2_PRIOR_KNOWLEDGE).let{ OkHttpClient.Builder().protocols(it).build() }
        val http1Client: OkHttpClient = OkHttpClient.Builder().build()
        val insecurePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        try {
            createTestApp { config: SslConfig ->
                config.secure = false
                config.http2 = false
                config.insecurePort = insecurePort
            }.start().let { _ ->
                Assertions.assertThrows(Exception::class.java) {
                    http2client.newCall(
                        Request.Builder().url(http).build()
                    ).execute()
                } // Should fail to connect using HTTP/2
                testSuccessfulEndpoint(http1Client, http, okhttp3.Protocol.HTTP_1_1)
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
    fun `Test that http2 can be disabled on the secure connector`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.http2 = false
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.securePort = securePort
                config.insecurePort = insecurePort
            }.start().let { _ -> testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_1_1) }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
    fun `Test that the insecure connector works with http2`() {
        val client: OkHttpClient =
            listOf(okhttp3.Protocol.H2_PRIOR_KNOWLEDGE).let { OkHttpClient.Builder().protocols(it).build() }
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        try {
            createTestApp { config: SslConfig ->
                config.secure = false
                config.http2 = true
                config.insecurePort = insecurePort
                config.securePort = securePort
            }.start().let { _ -> testSuccessfulEndpoint(client, http, okhttp3.Protocol.H2_PRIOR_KNOWLEDGE) }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
    fun `Test that the secure connector works with http2`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.securePort = securePort
                config.insecurePort = insecurePort
            }.start().let { _ -> testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_2) }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that by default both connectors are enabled, and that http1 and http2 works`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecurePort = insecurePort
                config.securePort = securePort
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
            }.start().let { _ ->
                testSuccessfulEndpoint(http, okhttp3.Protocol.HTTP_1_1)
                testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_2)
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the host can be changed`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecurePort = insecurePort
                config.securePort = securePort
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.host = "localhost"
            }.start().let { _ ->
                testSuccessfulEndpoint(http, okhttp3.Protocol.HTTP_1_1)
                testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_2)
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the host change fails when it doesn't match`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        try {
            createTestApp { config: SslConfig ->
                config.insecurePort = insecurePort
                config.securePort = securePort
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.host = "wronghost"
            }.start().let { _ -> Assertions.fail<Any>() }
        } catch (ignored: Exception) {}
    }

    @Test
     fun `Test that sniHostCheck works when it matches`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecurePort = insecurePort
                config.securePort = securePort
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.host = "localhost"
                config.sniHostCheck = true
            }.start().let { _ ->
                testSuccessfulEndpoint(http, okhttp3.Protocol.HTTP_1_1)
                testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_2)
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that sniHostCheck fails when it doesn't match over https`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecurePort = insecurePort
                config.securePort = securePort
                config.pemFromString(Server.GOOGLE_CERTIFICATE_AS_STRING, Server.GOOGLE_KEY_AS_STRING)
                config.host = "localhost"
                config.sniHostCheck = true
            }.start().let { _ ->
                //http request should be successful
                testSuccessfulEndpoint(untrustedClient, http, okhttp3.Protocol.HTTP_1_1)
                //https request should fail
                val wrongHttpsResponse = untrustedClient.newCall(Request.Builder().url(https).build()).execute()
                Assertions.assertEquals(400, wrongHttpsResponse.code)
                Assertions.assertTrue(
                    Objects.requireNonNull(wrongHttpsResponse.body)?.string()!!.contains("Error 400 Invalid SNI")
                )
                wrongHttpsResponse.close()
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that sniHostCheck can be disabled and a request with a wrong hostname can be made`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecurePort = insecurePort
                config.securePort = securePort
                config.pemFromString(Server.GOOGLE_CERTIFICATE_AS_STRING, Server.GOOGLE_KEY_AS_STRING)
                config.host = "localhost"
                config.sniHostCheck = false
            }.start().let { _ ->
                testSuccessfulEndpoint(untrustedClient, http, okhttp3.Protocol.HTTP_1_1)
                testSuccessfulEndpoint(untrustedClient, https, okhttp3.Protocol.HTTP_2)
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the connectors can be configured through the consumer`() {
        val insecurePort = ports.getAndIncrement()
        val securePort = ports.getAndIncrement()
        val http = HTTP_URL_WITH_PORT.apply(insecurePort)
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecurePort = insecurePort
                config.securePort = securePort
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.configConnectors { connector: ServerConnector ->
                    connector.setIdleTimeout(1000)
                    connector.name = "customName"
                }
            }.start().let { app ->
                testSuccessfulEndpoint(http, okhttp3.Protocol.HTTP_1_1)
                testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_2)
                for (connector in app.unsafeConfig().pvt.jetty.server!!.connectors) {
                    Assertions.assertEquals(1000, connector.idleTimeout)
                    Assertions.assertEquals("customName", connector.name)
                }
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the Security Provider can be automatically configured when the config is set to null`() {
        val securePort = ports.getAndIncrement()
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecure = false
                config.securePort = securePort
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
                config.securityProvider = null
            }.start().let { app ->
                printSecurityProviderName(app)
                testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_2)
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
     fun `Test that the Security Provider works when it is set to the default`() {
        val securePort = ports.getAndIncrement()
        val https = HTTPS_URL_WITH_PORT.apply(securePort)
        try {
            createTestApp { config: SslConfig ->
                config.insecure = false
                config.securePort = securePort
                config.pemFromString(Server.CERTIFICATE_AS_STRING, Server.NON_ENCRYPTED_KEY_AS_STRING)
            }.start().let { app ->
                printSecurityProviderName(app)
                testSuccessfulEndpoint(https, okhttp3.Protocol.HTTP_2)
            }
        } catch (e: IOException) {
            Assertions.fail<Any>(e)
        }
    }

    @Test
    fun `Test no identity loaded`() {
        Assertions.assertThrows(SslConfigException::class.java) {
            createTestApp { config: SslConfig ->
                config.insecure = false
                config.securePort = 8443
            }.start()
        }
        Assertions.assertTrue {
            try {
                createTestApp {
                    it.insecure = false
                    it.securePort = 8443
                }.start()
                return@assertTrue false
            } catch (e: SslConfigException) {
                return@assertTrue e.message?.contains(SslConfigException.Types.MISSING_CERT_AND_KEY_FILE.message) == true
            }
        }
    }

    companion object {
        private fun getSecurityProviderName(app: Javalin): String {
            val conn = app.jettyServer()!!.server().getConnectors()[0] as ServerConnector
            return conn.connectionFactories.stream()
                .filter { cf: ConnectionFactory? -> cf is SslConnectionFactory }
                .map { cf: ConnectionFactory -> cf as SslConnectionFactory }
                .map { sslConnectionFactory: SslConnectionFactory -> sslConnectionFactory.sslContextFactory.sslContext.provider.name }
                .findFirst()
                .orElseThrow()
        }

        private fun printSecurityProviderName(app: Javalin) {
            println("Security provider: " + getSecurityProviderName(app))
        }
    }
}
