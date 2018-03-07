/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

import io.javalin.core.JavalinServlet

/**
 * Creates an embedded server instance.
 *
 * @see <a href="https://javalin.io/documentation#custom-server">Custom server in docs</a>
 */
interface EmbeddedServerFactory {

    fun create(javalinServlet: JavalinServlet, staticFileConfig: List<StaticFileConfig>): EmbeddedServer
}
