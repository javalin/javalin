/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip
import io.javalin.http.Header
import io.javalin.http.ContentType
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.bundled.RouteOverviewPlugin
import io.javalin.testing.TestUtil
import jakarta.servlet.http.HttpSessionEvent
import jakarta.servlet.http.HttpSessionListener
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.junit.jupiter.api.Test

class TestConfiguration {

    @Test
    fun `all config options`() = TestUtil.runLogLess {
        val app = Javalin.create {
            it.spaRoot.addFile("/", "/public/html.html")
            it.spaRoot.addFile("/", "src/test/resources/public/html.html", Location.EXTERNAL)
            it.spaRoot.addHandler("/", {})
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.staticFiles.add("src/test/resources/public", Location.EXTERNAL)
            it.staticFiles.enableWebjars()
            it.plugins.register { }
            it.http.asyncTimeout = 10_000L
            it.http.generateEtags = true
            it.http.defaultContentType = ContentType.PLAIN
            it.plugins.enableCors { cors -> cors.add { it.reflectClientOrigin = true } }
            it.plugins.enableDevLogging()
            it.plugins.register(RouteOverviewPlugin("/test"))
            it.plugins.enableSslRedirects()
            it.http.prefer405over404 = false
            it.requestLogger.http { ctx, timeInMs -> }
            it.requestLogger.ws { ws -> }
            it.showJavalinBanner = false
            it.routing.contextPath = "/"
            it.jetty.sessionHandler { SessionHandler() }
            it.jetty.wsFactoryConfig { factory -> }
            it.jetty.server {
                Server()
            }
            it.jetty.contextHandlerConfig { handler ->
                handler.addEventListener(object : HttpSessionListener {
                    override fun sessionCreated(e: HttpSessionEvent?) {}
                    override fun sessionDestroyed(e: HttpSessionEvent?) {}
                })
            }
        }.start(0)
        assertThat(app.jettyServer.started).isTrue()
        app.stop()
    }

    @Test
    fun `compression strategy is set to gzip by default`() {
        val app = Javalin.create()
        assertThat(app.cfg.pvt.compressionStrategy).isEqualTo(CompressionStrategy.GZIP)
    }

    @Test
    fun `compression strategy can be customized by user`() {
        val app = Javalin.create {
            it.compression.custom(CompressionStrategy(null, Gzip(2)))
        }
        assertThat(app.cfg.pvt.compressionStrategy.gzip?.level).isEqualTo(2)
        assertThat(app.cfg.pvt.compressionStrategy.brotli).isNull()
    }

    @Test
    fun `app throws exception saying port is busy if it is`() = TestUtil.test { app, _ ->
        Assertions.assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create().start(app.port()) }
            .withMessageContaining("Port already in use. Make sure no other process is using port ${app.port()} and try again")
    }

    @Test
    fun `contextResolvers config with custom settings`() {
        TestUtil.test(
            Javalin.create {
                it.contextResolver.ip = { "CUSTOM IP" }
                it.contextResolver.host = { "CUSTOM HOST" }
            }
                .get("/ip") { it.result(it.ip()) }
                .get("/host") { it.result("${it.host()}") }
        ) { _, http ->
            assertThat(http.get("/ip").body).isEqualTo("CUSTOM IP")
            assertThat(http.get("/host").body).isEqualTo("CUSTOM HOST")
        }
    }

    @Test
    fun `contextResolvers config with default settings`() {
        TestUtil.test(
            Javalin.create {}
                .get("/ip") { it.result(it.ip()) }
                .get("/remote-ip") { it.result(it.req().remoteAddr) }
                .get("/host") { it.result("${it.host()}") }
                .get("/remote-host") { it.result("${it.header(Header.HOST)}") }
        ) { _, http ->
            assertThat(http.get("/ip").body).isEqualTo(http.get("/remote-ip").body)
            assertThat(http.get("/host").body).isEqualTo(http.get("/remote-host").body)
        }
    }
}
