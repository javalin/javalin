/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

// This is a very basic example, a better one can be found at:
// https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java#L139-L163
fun main() {
    val app = Javalin.create {
        it.jetty.addConnector { server, _ ->
            val sslConnector = ServerConnector(server, sslContextFactory())
            sslConnector.port = 443
            sslConnector
        }
        it.jetty.addConnector { server, _ ->
            val httpConnector = ServerConnector(server)
            httpConnector.port = 80
            httpConnector
        }
    }.start()

    app.get("/") { it.result("Hello World") } // valid endpoint for both connectors
}

private fun sslContextFactory(): SslContextFactory.Server {
    val sslContextFactory = SslContextFactory.Server()
    sslContextFactory.keyStorePath = HelloWorldSecure::class.java.getResource("/keystore.jks")!!.toExternalForm()
    sslContextFactory.keyStorePassword = "localhost"
    return sslContextFactory
}
