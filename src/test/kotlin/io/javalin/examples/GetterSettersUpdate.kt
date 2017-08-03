package io.javalin.examples

import io.javalin.Javalin
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector


fun main(args: Array<String>) {
    val app = Javalin
            .create()
            .apply {
                port = 0
                staticFileLocation = "/"
            }


    app.get("/") { ctx -> ctx.result("Hello World") }
}