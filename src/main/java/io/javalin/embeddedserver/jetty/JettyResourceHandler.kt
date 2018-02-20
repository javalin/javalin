/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import io.javalin.HaltException
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

class JettyResourceHandler(staticFileConfig: List<StaticFileConfig>) : StaticResourceHandler {

    private val log = LoggerFactory.getLogger(JettyResourceHandler::class.java)

    private val handlers = staticFileConfig.map { config ->
        GzipHandler().apply {
            handler = ResourceHandler().apply {
                resourceBase = getResourcePath(config)
                isDirAllowed = false
                isEtags = true
                start()
            }
            log.info("Static files enabled: {$config}. Absolute path: '${getResourcePath(config)}'")
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
        val target = httpRequest.getAttribute("jetty-target") as String
        val baseRequest = httpRequest.getAttribute("jetty-request") as Request
        for (gzipHandler in handlers) {
            try {
                val resourceHandler = (gzipHandler.handler as ResourceHandler)
                val resource = resourceHandler.getResource(target)
                if (resource.isFile() || resource.isDirectoryWithWelcomeFile(resourceHandler, target)) {
                    val maxAge = if (target.startsWith("/immutable")) 31622400 else 0
                    httpResponse.setHeader("Cache-Control", "max-age=$maxAge")
                    gzipHandler.handle(target, baseRequest, httpRequest, httpResponse)
                    return true
                }
            } catch (e: Exception) { // it's fine
                log.error("Exception occurred while handling static resource", e)
            }
        }
        throw HaltException(404, "Not found")
    }

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory
    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
            this != null && this.isDirectory && handler.getResource(target + "index.html").exists()

}
