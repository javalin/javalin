/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.undertow

import io.javalin.core.JavalinServlet
import io.javalin.embeddedserver.EmbeddedServer
import io.javalin.embeddedserver.EmbeddedServerFactory
import io.javalin.embeddedserver.StaticFileConfig
import io.javalin.embeddedserver.jetty.JettyResourceHandler
import io.undertow.Undertow

class EmbeddedUndertowFactory(undertowServer: () -> Undertow = { Undertow.builder().build() }) : EmbeddedServerFactory {
    private val undertow = undertowServer()
    override fun create(javalinServlet: JavalinServlet, staticFileConfig: StaticFileConfig?): EmbeddedServer {
        return EmbeddedUndertowServer(undertow, javalinServlet.apply { staticResourceHandler = JettyResourceHandler(staticFileConfig) })
    }
}
