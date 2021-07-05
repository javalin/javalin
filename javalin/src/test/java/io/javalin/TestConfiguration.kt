/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.metrics.MicrometerPlugin
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.junit.Test
import javax.servlet.http.HttpSessionEvent
import javax.servlet.http.HttpSessionListener

class TestConfiguration {

    @Test
    fun `test all config options`() {
        val app = Javalin.create {
            // JavalinServlet
            it.addSinglePageRoot("/", "/public/html.html")
            it.addSinglePageRoot("/", "src/test/resources/public/html.html", Location.EXTERNAL)
            it.addStaticFiles("/public", Location.CLASSPATH)
            it.addStaticFiles("src/test/resources/public", Location.EXTERNAL)
            it.asyncRequestTimeout = 10_000L
            it.autogenerateEtags = true
            it.contextPath = "/"
            it.defaultContentType = "text/plain"
            it.enableCorsForAllOrigins()
            it.enableDevLogging()
            it.registerPlugin(RouteOverviewPlugin("/test"))
            it.enableWebjars()
            it.enforceSsl = true
            it.logIfServerNotStarted = false
            it.prefer405over404 = false
            it.requestLogger { ctx, executionTimeMs -> }
            it.sessionHandler { SessionHandler() }
            // WsServlet
            it.wsFactoryConfig { }
            it.wsLogger { }
            // Server
            it.server {
                Server()
            }
            it.registerPlugin(MicrometerPlugin())
            // Misc
            it.accessManager { handler, ctx, roles -> }
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
    fun `app throws exception saying port is busy if it is`() = TestUtil.test { app, http ->
        Assertions.assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create().start(app.port()) }
            .withMessageContaining("Port already in use. Make sure no other process is using port ${app.port()} and try again")
    }
}
