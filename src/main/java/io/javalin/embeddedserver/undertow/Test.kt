/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.undertow

import io.undertow.Handlers
import io.undertow.Undertow
import io.undertow.servlet.Servlets

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object Test {
    @JvmStatic fun main(args: Array<String>) {
        val servletBuilder = Servlets.deployment().setClassLoader(Test::class.java.classLoader)
                .setDeploymentName("myapp").setContextPath("/myapp")
                .addServlets(Servlets.servlet("myservlet",
                        object : HttpServlet() {
                            override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
                                response.writer.write("Hello World!")
                            }
                        }.javaClass).addMapping("/myservlet"))
        val manager = Servlets.defaultContainer().addDeployment(servletBuilder)
        manager.deploy()
        val path = Handlers.path(Handlers.redirect("/myapp")).addPrefixPath("/myapp", manager.start())
        val server = Undertow.builder().addHttpListener(8888, "localhost").setHandler(path).build()
        server.start()
    }
}
