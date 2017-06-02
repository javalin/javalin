/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import java.io.IOException

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.session.SessionHandler

import io.javalin.core.JavalinServlet
import io.javalin.embeddedserver.CachedRequestWrapper

class JettyHandler(private val javalinServlet: JavalinServlet) : SessionHandler() {

    @Throws(IOException::class, ServletException::class)
    override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val cachedRequest = CachedRequestWrapper(request)
        cachedRequest.setAttribute("jetty-target", target)
        cachedRequest.setAttribute("jetty-request", jettyRequest)
        javalinServlet.service(cachedRequest, response)
        jettyRequest.isHandled = true
    }

}
