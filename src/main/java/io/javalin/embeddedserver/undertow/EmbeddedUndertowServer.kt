/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.undertow;

import io.javalin.core.JavalinServlet
import io.javalin.embeddedserver.CachedRequestWrapper
import io.javalin.embeddedserver.EmbeddedServer
import io.undertow.Handlers
import io.undertow.Undertow
import io.undertow.servlet.Servlets
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class EmbeddedUndertowServer(private var undertow: Undertow, private val javalinServlet: JavalinServlet) : EmbeddedServer {

    override fun start(host: String, port: Int): Int {
        undertow = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(Handlers.path(Handlers.redirect("/")).addPrefixPath("/",
                        Servlets.defaultContainer().addDeployment(
                                Servlets.deployment()
                                        .setClassLoader(EmbeddedUndertowServer::class.java.classLoader)
                                        .setDeploymentName("javalinDeployment").setContextPath("/")
                                        .addServlets(Servlets.servlet("javalinServlet",
                                                object : HttpServlet() {
                                                    override fun service(request: HttpServletRequest, response: HttpServletResponse) {
                                                        // response.writer.write("Hello World!") // works
                                                        javalinServlet.service(CachedRequestWrapper(request), response) // doesn't work
                                                    }
                                                }.javaClass).addMapping("/")) // need to init
                        ).apply { deploy() }.start()
                ))
                .build()
        undertow.start()
        return port
    }

    override fun stop() = undertow.stop()
    override fun activeThreadCount() = -1
    override fun attribute(key: String): Any = -1
}


