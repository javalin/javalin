/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip
import io.javalin.core.util.Header
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.ContentType
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.metrics.MicrometerPlugin
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.junit.jupiter.api.Test
import jakarta.servlet.http.HttpSessionEvent
import jakarta.servlet.http.HttpSessionListener

class TestConfiguration {

    @Test
    fun `test all config options`() = TestUtil.runLogLess {
        val app = Javalin.create {

            // JavalinServlet
            it.singlePage.addRootFile("/", "/public/html.html")
            it.singlePage.addRootFile("/", "src/test/resources/public/html.html", Location.EXTERNAL)
            it.singlePage.addRootHandler("/", {})
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.staticFiles.add("src/test/resources/public", Location.EXTERNAL)
            it.staticFiles.enableWebjars()
            it.registerPlugin {  }
            it.asyncRequestTimeout = 10_000L
            it.autogenerateEtags = true
            it.defaultContentType = ContentType.PLAIN
            it.enableCorsForAllOrigins()
            it.enableDevLogging()
            it.registerPlugin(RouteOverviewPlugin("/test"))
            it.enforceSsl = true
            it.prefer405over404 = false
            it.requestLogger { ctx, timeInMs -> }
            it.wsLogger { ws -> }
            it.registerPlugin(MicrometerPlugin())
            // Misc
            it.accessManager { _, _, _ -> }
            it.showJavalinBanner = false
            it.jetty.contextPath = "/"
            it.jetty.sessionHandler { SessionHandler() }
            it.jetty.wsFactoryConfig { factory -> }
            it.jetty.server {
                Server()
            }
            it.jetty.contextHandlerConfig { handler ->
                handler.addEventListener(object : HttpSessionListener {
                    override fun sessionCreated(e: HttpSessionEvent?) {
                    }

                    override fun sessionDestroyed(e: HttpSessionEvent?) {
                    }
                })
            }
        }.start(0)
        assertThat(app.jettyServer.started).isTrue()
        app.stop()
    }

    @Test
    fun `compression strategy is set to gzip by default`() {
        val app = Javalin.create()
        assertThat(app.cfg.inner.compressionStrategy).isEqualTo(CompressionStrategy.GZIP)
    }

    @Test
    fun `compression strategy can be customized by user`() {
        val app = Javalin.create {
            it.compressionStrategy(null, Gzip(2))
        }
        assertThat(app.cfg.inner.compressionStrategy.gzip?.level).isEqualTo(2)
        assertThat(app.cfg.inner.compressionStrategy.brotli).isNull()
    }

    @Test
    fun `app throws exception saying port is busy if it is`() = TestUtil.test { app, _ ->
        Assertions.assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create().start(app.port()) }
            .withMessageContaining("Port already in use. Make sure no other process is using port ${app.port()} and try again")
    }

    @Test
    fun `test contextResolvers config with custom settings`() {
        TestUtil.test(
            Javalin.create {
                it.contextResolvers { resolvers ->
                    resolvers.ip = { "CUSTOM IP" }
                    resolvers.host = { "CUSTOM HOST" }
                }
            }
                .get("/ip") { it.result(it.ip()) }
                .get("/host") { it.result("${it.host()}") }
        ) { _, http ->
            assertThat(http.get("/ip").body).isEqualTo("CUSTOM IP")
            assertThat(http.get("/host").body).isEqualTo("CUSTOM HOST")
        }
    }

    @Test
    fun `test contextResolvers config with default settings`() {
        TestUtil.test(
            Javalin.create {}
                .get("/ip") { it.result(it.ip()) }
                .get("/remote-ip") { it.result(it.req.remoteAddr) }
                .get("/host") { it.result("${it.host()}") }
                .get("/remote-host") { it.result("${it.header(Header.HOST)}") }
        ) { _, http ->
            assertThat(http.get("/ip").body).isEqualTo(http.get("/remote-ip").body)
            assertThat(http.get("/host").body).isEqualTo(http.get("/remote-host").body)
        }
    }
}
