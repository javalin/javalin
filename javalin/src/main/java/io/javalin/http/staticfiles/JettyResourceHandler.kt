/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.staticfiles

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.core.util.Util
import io.javalin.http.JavalinResponseWrapper
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.resource.EmptyResource
import org.eclipse.jetty.util.resource.Resource
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyResourceHandler(val precompressStaticFiles: Boolean = false) : io.javalin.http.staticfiles.ResourceHandler {

    val handlers = mutableListOf<ResourceHandler>()

    override fun addStaticFileConfig(config: StaticFileConfig) {
        val handler = if (config.path == "/webjars") WebjarHandler() else PrefixableHandler(config.urlPathPrefix).apply {
            resourceBase = getResourcePath(config)
            isDirAllowed = false
            isEtags = true
            Javalin.log?.info("""Static file handler added:
                |    {urlPathPrefix: "${config.urlPathPrefix}", path: "${config.path}", location: Location.${config.location}}
                |    Resolved path: '${getResourcePath(config)}'
                """.trimMargin())
        }
        handlers.add(handler.apply { start() })
    }

    inner class WebjarHandler : ResourceHandler() {
        override fun getResource(path: String) = Resource.newClassPathResource("META-INF/resources$path") ?: super.getResource(path)
    }

    inner class PrefixableHandler(private var urlPathPrefix: String) : ResourceHandler() {
        override fun getResource(path: String): Resource {
            val targetResource by lazy { path.removePrefix(urlPathPrefix) }
            return when {
                urlPathPrefix == "/" -> super.getResource(path) // same as regular ResourceHandler
                targetResource == "" -> super.getResource("/") // directory without trailing '/'
                !path.startsWith(urlPathPrefix) -> EmptyResource.INSTANCE
                !targetResource.startsWith("/") -> EmptyResource.INSTANCE
                else -> super.getResource(targetResource)
            }
        }
    }

    private fun getResourcePath(staticFileConfig: StaticFileConfig): String {
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

    override fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean {
        val target = httpRequest.getAttribute("jetty-target") as String
        val baseRequest = httpRequest.getAttribute("jetty-request") as Request
        for (handler in handlers) {
            try {
                val resource = handler.getResource(target)
                if (resource.isFile() || resource.isDirectoryWithWelcomeFile(handler, target)) {
                    val maxAge = if (target.startsWith("/immutable/") || handler is WebjarHandler) 31622400 else 0
                    httpResponse.setHeader(Header.CACHE_CONTROL, "max-age=$maxAge")
                    // Remove the default content type because Jetty will not set the correct one
                    // if the HTTP response already has a content type set
                    if (precompressStaticFiles && PrecompressingResourceHandler.handle(resource, httpRequest, httpResponse)) {
                        return true
                    }
                    httpResponse.contentType = null
                    handler.handle(target, baseRequest, httpRequest, httpResponse)
                    httpRequest.setAttribute("handled-as-static-file", true)
                    (httpResponse as JavalinResponseWrapper).outputStream.finalize()
                    return true
                }
            } catch (e: Exception) { // it's fine
                if (!Util.isClientAbortException(e)) {
                    Javalin.log?.error("Exception occurred while handling static resource", e)
                }
            }
        }
        return false
    }

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory

    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
            this != null && this.isDirectory && handler.getResource("${target.removeSuffix("/")}/index.html")?.exists() == true

}
