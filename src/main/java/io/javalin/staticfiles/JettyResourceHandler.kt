/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.staticfiles

import io.javalin.core.util.Header
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.util.resource.Resource
import org.slf4j.LoggerFactory
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

data class StaticFileConfig(val path: String, val location: Location)
enum class Location { CLASSPATH, EXTERNAL; }

class JettyResourceHandler(staticFileConfig: List<StaticFileConfig>) {

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
        if (!File(staticFileConfig.path).exists()) {
            throw RuntimeException(nosuchdir)
        }
        return staticFileConfig.path
    }

    fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) {
        val target = httpRequest.getAttribute("jetty-target") as String
        val baseRequest = httpRequest.getAttribute("jetty-request") as Request
        for (gzipHandler in handlers) {
            try {
                val resourceHandler = (gzipHandler.handler as ResourceHandler)
                val resource = resourceHandler.getResource(target)
                if (resource.isFile() || resource.isDirectoryWithWelcomeFile(resourceHandler, target)) {
                    val maxAge = if (target.startsWith("/immutable")) 31622400 else 0
                    httpResponse.setHeader(Header.CACHE_CONTROL, "max-age=$maxAge")
                    gzipHandler.handle(target, baseRequest, httpRequest, httpResponse)
                }
            } catch (e: Exception) { // it's fine
                log.error("Exception occurred while handling static resource", e)
            }
        }
    }

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory
    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
            this != null && this.isDirectory && handler.getResource(target + "index.html").exists()

}
