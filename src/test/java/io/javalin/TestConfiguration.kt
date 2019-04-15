/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.staticfiles.Location
import io.javalin.util.TestUtil
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.junit.Test

class TestConfiguration {

    @Test
    fun `test all config options`() {
        val app = Javalin.create {
            it.addSinglePageRoot("/", "/public/html.html")
            it.addSinglePageRoot("/", "src/test/resources/public/html.html", Location.EXTERNAL)
            it.addStaticFiles("/public")
            it.addStaticFiles("src/test/resources/public", Location.EXTERNAL)
            it.contextPath = "/"
            it.defaultContentType = "text/plain"
            it.dynamicGzip = true
            it.enableWebjars()
            it.enableCorsForOrigin("*", "my-origin")
            it.prefer405over404 = false
            it.autogenerateEtags = true
            it.requestCacheSize = 8192L
            it.requestLogger { ctx, executionTimeMs -> }
            it.accessManager { handler, ctx, permittedRoles -> }
            it.sessionHandler { SessionHandler() }
            it.wsContextPath = "/"
            it.wsFactoryConfig { }
            it.wsLogger { }
            it.server {
                Server()
            }
        }
    }

}
