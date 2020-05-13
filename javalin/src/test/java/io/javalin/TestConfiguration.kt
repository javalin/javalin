/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.metrics.MicrometerPlugin
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
            it.addStaticFiles("/public")
            it.addStaticFiles("src/test/resources/public", Location.EXTERNAL)
            it.asyncRequestTimeout = 10_000L
            it.autogenerateEtags = true
            it.contextPath = "/"
            it.defaultContentType = "text/plain"
            it.dynamicGzip = true
            it.enableCorsForAllOrigins()
            it.enableDevLogging()
            it.registerPlugin(RouteOverviewPlugin("/test", roles()))
            it.enableWebjars()
            it.enforceSsl = true
            it.logIfServerNotStarted = false
            it.prefer405over404 = false
            it.requestCacheSize = 8192L
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
            it.accessManager { handler, ctx, permittedRoles -> }
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
        assertThat(app.server.started).isTrue()
        app.stop()
    }

    @Test
    fun `compression strategy with default config is correct`() {
        val app = Javalin.create()
        assertThat(app.config.inner.compressionStrategy).isEqualTo(CompressionStrategy.GZIP)
    }

    @Test
    fun `compression strategy can be customized by user`() {
        val app = Javalin.create {
            it.compressionStrategy(null, Gzip(2))
        }
        assertThat(app.config.inner.compressionStrategy.gzip?.level).isEqualTo(2)
        assertThat(app.config.inner.compressionStrategy.brotli).isNull()
    }

    @Test
    fun `compression strategy gets disabled when dynamicGzip is set to false`() {
        val app = Javalin.create {
            it.dynamicGzip = false
            it.compressionStrategy(null, Gzip(8))
        }
        assertThat(app.config.inner.compressionStrategy).isEqualTo(CompressionStrategy.NONE)
    }
}
