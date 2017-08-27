/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import io.javalin.embeddedserver.Location
import io.javalin.embeddedserver.StaticFileConfig
import io.javalin.embeddedserver.StaticResourceHandler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.util.resource.Resource
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyResourceHandler(staticFileConfig: StaticFileConfig?) : StaticResourceHandler {

    private val log = LoggerFactory.getLogger(JettyResourceHandler::class.java)

    private var initialized = false
    private val resourceHandler = ResourceHandler()
    private val gzipHandler = GzipHandler().apply { handler = resourceHandler }

    init {
        if (staticFileConfig != null) {
            resourceHandler.apply {
                resourceBase = getResourcePath(staticFileConfig)
                isDirAllowed = false
                isEtags = true
            }.start()
            initialized = true
            log.info("Static files enabled: {$staticFileConfig}. Absolute path: '${resourceHandler.resourceBase}'")
        }
    }

    fun getResourcePath(staticFileConfig: StaticFileConfig): String {
        val nosuchdir = "Static resource directory with path: '${staticFileConfig.path}' does not exist."
        if (staticFileConfig.location == Location.CLASSPATH) {
            val classPathResource = Resource.newClassPathResource(staticFileConfig.path)
            if (classPathResource == null) {
                throw RuntimeException(nosuchdir + " Depending on your setup, empty folders might not get copied to classpath.")
            }
            return classPathResource.toString()
        }
        if (Files.notExists(Paths.get(staticFileConfig.path))) {
            throw RuntimeException(nosuchdir)
        }
        return staticFileConfig.path
    }

    override fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean {
        if (initialized) {
            val target = httpRequest.getAttribute("jetty-target") as String
            val baseRequest = httpRequest.getAttribute("jetty-request") as Request // org.eclipse.jetty.server.Request
            try {
                if (!resourceHandler.getResource(target).isDirectory || resourceHandler.getResource(target + "index.html").exists()) {
                    val maxAge = if (target.startsWith("/immutable")) 31622400 else 0
                    httpResponse.setHeader("Cache-Control", "max-age=$maxAge")
                    gzipHandler.handle(target, baseRequest, httpRequest, httpResponse)
                    return true;
                }
            } catch (e: Exception) { // it's fine
                log.error("Exception occurred while handling static resource", e)
            }
        }
        return false
    }

}
