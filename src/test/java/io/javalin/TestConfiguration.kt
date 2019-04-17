/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.security.SecurityUtil.roles
import io.javalin.staticfiles.Location
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.junit.Test

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
            it.enableCorsForOrigin("*", "my-origin")
            it.enableDevLogging()
            it.enableRouteOverview("/test")
            it.enableRouteOverview("/test", roles())
            it.enableWebjars()
            it.enforceSsl = true
            it.prefer405over404 = false
            it.requestCacheSize = 8192L
            it.requestLogger { ctx, executionTimeMs -> }
            it.sessionHandler { SessionHandler() }
            // WsServlet
            it.wsContextPath = "/"
            it.wsFactoryConfig { }
            it.wsLogger { }
            // Server
            it.server {
                Server()
            }
            // Misc
            it.accessManager { handler, ctx, permittedRoles -> }
            it.showJavalinBanner = false
        }.start()
        assertThat(app.server.started).isTrue()
        app.stop()
    }

}
