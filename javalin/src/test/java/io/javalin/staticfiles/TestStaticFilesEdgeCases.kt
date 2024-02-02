/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestEnvironment
import io.javalin.testing.TestUtil
import io.javalin.util.FileUtil
import kong.unirest.Unirest
import okhttp3.OkHttpClient
import okhttp3.Protocol.H2_PRIOR_KNOWLEDGE
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.ServerSocket
import java.net.http.HttpClient
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager
import kotlin.io.path.Path

class TestStaticFilesEdgeCases {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `server doesn't start for non-existent classpath folder`() = TestUtil.runLogLess {
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create { it.staticFiles.add("classpath-fake-folder", Location.CLASSPATH) }.start(0) }
            .withMessageContaining("Static resource directory with path: 'classpath-fake-folder' does not exist.")
    }

    @Test
    fun `server doesn't start for non-existent external folder`() = TestUtil.runLogLess {
        val workingDirectory = Path(System.getProperty("user.dir"))
        val fullExternalFakeFolderPath = workingDirectory.resolve("external-fake-folder")
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create { it.staticFiles.add("external-fake-folder", Location.EXTERNAL) }.start(0) }
            .withMessageContaining("Static resource directory with path: '$fullExternalFakeFolderPath' does not exist.")
    }

    @Test
    fun `server doesn't start for empty classpath folder`() = TestUtil.runLogLess {
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create { it.staticFiles.add(workingDirectory.absolutePath, Location.CLASSPATH) }.start(0) }
            .withMessageContaining("Static resource directory with path: '${workingDirectory.absolutePath}' does not exist.")
            .withMessageContaining("Depending on your setup, empty folders might not get copied to classpath.")
    }

    @Test
    fun `server starts for empty external folder`() = TestUtil.runLogLess {
        Javalin.create { it.staticFiles.add(workingDirectory.absolutePath, Location.EXTERNAL) }.start(0).stop()
    }

    @Test
    fun `test FileUtil`() {
        assertThat(FileUtil.readFile("src/test/external/html.html")).contains("<h1>HTML works</h1>")
        assertThat(FileUtil.readResource("/public/html.html")).contains("<h1>HTML works</h1>")
    }

    @Test
    fun `static files works on http2 no ssl `() {
        // test http1 first:
        val http1App = Javalin.create { it.staticFiles.add("/public") }
        TestUtil.test(http1App) { _, http ->
            assertThat(http.getBody("/styles.css")).contains("CSS works")
        }
        // then test http2:
        val port = ServerSocket(0).use { it.localPort }
        val http2App = Javalin.create {
            it.staticFiles.add("/public")
            it.jetty.addConnector { server, httpConfiguration ->
                val http11 = HttpConnectionFactory(httpConfiguration)
                val http2 = HTTP2CServerConnectionFactory(httpConfiguration)
                ServerConnector(server, http11, http2).apply {
                    this.port = port
                }
            }
        }
        TestUtil.test(http2App) { app, http ->
            val http2client = listOf(H2_PRIOR_KNOWLEDGE).let { OkHttpClient.Builder().protocols(it).build() }
            val path = "http://127.0.0.1:$port/styles.css"
            val response = http2client.newCall(Request.Builder().url(path).build()).execute()
            assertThat(response.body?.string()).contains("CSS works")
            assertThat(response.protocol).isEqualTo(H2_PRIOR_KNOWLEDGE)
        }
    }

    @Test
    fun `static files works on http2 with ssl and ALPN`() {
        val port = ServerSocket(0).use { it.localPort }
        val http2App = Javalin.create {
            it.staticFiles.add("/public")
            it.jetty.addConnector { server, httpConfiguration ->
                httpConfiguration.addCustomizer(SecureRequestCustomizer(false))
                val http11 = HttpConnectionFactory(httpConfiguration)
                val http2 = HTTP2ServerConnectionFactory(httpConfiguration)
                val alpn = ALPNServerConnectionFactory()
                alpn.setDefaultProtocol(http11.protocol)
                val sslContextFactory = SslContextFactory.Server().apply {
                    keyStorePath = this::class.java.getResource("/keystore.jks")!!.toExternalForm()
                    keyStorePassword = "localhost"
                }
                val tlsHttp2 = SslConnectionFactory(sslContextFactory, alpn.protocol)
                ServerConnector(server, tlsHttp2, alpn, http2, http11).apply { this.port = port }
            }
        }
        TestUtil.test(http2App) { app, http ->
            val client = untrustedClient()
            val emptyPath = "https://127.0.0.1:$port/idontexist.css"
            val emptyResponse = client.newCall(Request.Builder().url(emptyPath).build()).execute()
            assertThat(emptyResponse.code).isEqualTo(404)
            val path = "https://127.0.0.1:$port/styles.css"
            val response = client.newCall(Request.Builder().url(path).build()).execute()
            assertThat(response.body.toString()).contains("CSS works")
        }
    }

    @Test
    fun `static files works on https`() {
        if (TestEnvironment.isMac && TestEnvironment.isCiServer) return // TODO: no idea why this fails on mac ci
        val port = ServerSocket(0).use { it.localPort }
        Javalin.create {
            it.staticFiles.add("/public")
            it.jetty.addConnector { server, _ ->
                ServerConnector(server, SslContextFactory.Server().apply {
                    keyStorePath = this::class.java.getResource("/keystore.jks")!!.toExternalForm()
                    keyStorePassword = "localhost"
                }).apply { this.port = port }
            }
            it.jetty.addConnector { server, _ ->
                ServerConnector(server).apply { this.port = 0 }
            }
        }.start().also {
            Unirest.config().verifySsl(false)
            val response = Unirest.get("https://localhost:$port/styles.css").asString()
            assertThat(response.body).contains("CSS works")
        }.stop()
    }

    /** Needed to accept self-signed certificates in okhttp...
     */
     fun untrustedClient(): OkHttpClient {
         val trustAllCerts = arrayOf<X509TrustManager>(object : X509TrustManager {
             override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
             override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
             override fun getAcceptedIssuers(): Array<X509Certificate> {
                 return arrayOf()
             }
         })
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
        return newBuilder.build()
    }

}
