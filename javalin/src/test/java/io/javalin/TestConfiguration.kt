/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip
import io.javalin.core.util.Header
import io.javalin.core.util.JavalinLogger
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.metrics.MicrometerPlugin
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.junit.jupiter.api.Test
import javax.servlet.http.HttpSessionEvent
import javax.servlet.http.HttpSessionListener

class TestConfiguration {

    @Test
    fun `test all config options`() {
        val app = Javalin.create {
            // JavalinServlet
            it.addSinglePageRoot("/", "/public/html.html")
            it.addSinglePageRoot("/", "src/test/resources/public/html.html", Location.EXTERNAL)
            it.addSinglePageHandler("/", {})
            it.addStaticFiles("/public", Location.CLASSPATH)
            it.addStaticFiles("src/test/resources/public", Location.EXTERNAL)
            it.asyncRequestTimeout = 10_000L
            it.autogenerateEtags = true
            it.contextPath = "/"
            it.defaultContentType = ContentType.PLAIN
            it.enableCorsForAllOrigins()
            it.enableDevLogging()
            it.registerPlugin(RouteOverviewPlugin("/test"))
            it.enableWebjars()
            it.enforceSsl = true
            it.prefer405over404 = false
            it.requestLogger { ctx, timeInMs -> }
            it.sessionHandler { SessionHandler() }
            // WsServlet
            it.wsFactoryConfig { factory -> }
            it.wsLogger { ws -> }
            // Server
            it.server {
                Server()
            }
            it.registerPlugin(MicrometerPlugin())
            // Misc
            it.accessManager { _, _, _ -> }
            it.showJavalinBanner = false
            it.configureServletContextHandler { handler ->
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
        assertThat(app._conf.inner.compressionStrategy).isEqualTo(CompressionStrategy.GZIP)
    }

    @Test
    fun `compression strategy can be customized by user`() {
        val app = Javalin.create {
            it.compressionStrategy(null, Gzip(2))
        }
        assertThat(app._conf.inner.compressionStrategy.gzip?.level).isEqualTo(2)
        assertThat(app._conf.inner.compressionStrategy.brotli).isNull()
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
