/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

// This is a very basic example, a better one can be found at:
// https://github.com/eclipse/jetty.project/blob/jetty-9.4.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java#L139-L163
fun main(args: Array<String>) {
    val app = Javalin.create {
        it.server {
            val server = Server()
            val sslConnector = ServerConnector(server, sslContextFactory())
            sslConnector.port = 443
            val connector = ServerConnector(server)
            connector.port = 80
            server.connectors = arrayOf<Connector>(sslConnector, connector)
            server
        }
    }.start()

    app.get("/") { ctx -> ctx.result("Hello World") } // valid endpoint for both connectors
}

private fun sslContextFactory(): SslContextFactory {
    val sslContextFactory = SslContextFactory()
    sslContextFactory.keyStorePath = HelloWorldSecure::class.java.getResource("/keystore.jks").toExternalForm()
    sslContextFactory.setKeyStorePassword("password")
    return sslContextFactory
}

