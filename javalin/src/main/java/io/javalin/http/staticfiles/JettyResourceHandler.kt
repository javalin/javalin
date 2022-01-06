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
import org.eclipse.jetty.server.handler.ContextHandler.AliasCheck
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.resource.EmptyResource
import org.eclipse.jetty.util.resource.Resource
import java.io.File
import java.nio.file.AccessDeniedException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse
import io.javalin.http.staticfiles.ResourceHandler as JavalinResourceHandler

class JettyResourceHandler(val precompressStaticFiles: Boolean = false, private val aliasCheck: AliasCheck? = null) : JavalinResourceHandler {

    val handlers = mutableListOf<ResourceHandler>()

    override fun addStaticFileConfig(config: StaticFileConfig) {
        handlers.add(when {
            config.path == "/webjars" -> WebjarHandler()
            aliasCheck != null -> AliasHandler(config, aliasCheck)
            else -> PrefixableHandler(config)
        }.apply { start() })
    }

    override fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean {
        val target = httpRequest.getAttribute("jetty-target") as String
        val baseRequest = httpRequest.getAttribute("jetty-request") as Request
        for (handler in handlers) {
            try {
                val resource = handler.getResource(target)
                val hasDirectoryWithWelcomeFile = resource.isDirectoryWithWelcomeFile(handler, target)
                if (resource.isFile() || hasDirectoryWithWelcomeFile) {
                    val maxAge = if (target.startsWith("/immutable/") || handler is WebjarHandler) 31622400 else 0
                    httpResponse.setHeader(Header.CACHE_CONTROL, "max-age=$maxAge")
                    // Remove the default content type because Jetty will not set the correct one
                    // if the HTTP response already has a content type set
                    if (precompressStaticFiles && PrecompressingResourceHandler.handle(resource, httpRequest, httpResponse)) {
                        return true
                    }
                    httpResponse.contentType = null
                    val req = if (hasDirectoryWithWelcomeFile) welcomeDirectoryHttpRequest(httpRequest) else httpRequest
                    handler.handle(target, baseRequest, req, httpResponse)
                    httpRequest.setAttribute("handled-as-static-file", true)
                    (httpResponse as JavalinResponseWrapper).outputStream.finalize()
                    return true
                }
            } catch (e: Exception) { // it's fine, we'll just 404
                if (!Util.isClientAbortException(e)) {
                    Javalin.log?.info("Exception occurred while handling static resource", e)
                }
            }
        }
        return false
    }

    private fun welcomeDirectoryHttpRequest(req: HttpServletRequest): HttpServletRequest {
        val welcomeDirectory = req.pathInfo
        return object : HttpServletRequestWrapper(req) {
            override fun getPathInfo() = when (val pathInfo = super.getPathInfo()) {
                welcomeDirectory -> addSlashSuffix(pathInfo)
                else -> pathInfo
            }
        }
    }

    private fun addSlashSuffix(path: String) = "${path.removeSuffix("/")}/"

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory

    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
            this != null && this.isDirectory && handler.getResource("${target.removeSuffix("/")}/index.html")?.exists() == true
}

private class WebjarHandler : ResourceHandler() {
    override fun getResource(path: String) = Resource.newClassPathResource("META-INF/resources$path") ?: super.getResource(path)
}

private open class PrefixableHandler(private val config: StaticFileConfig) : ResourceHandler() {

    init {
        resourceBase = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        Javalin.log?.info("""Static file handler added:
        |    {urlPathPrefix: "${config.urlPathPrefix}", path: "${config.path}", location: Location.${config.location}}
        |    Resolved path: '${getResourceBase(config)}'
        """.trimMargin())
    }

    private fun getResourceBase(config: StaticFileConfig): String {
        val noSuchDirMessage = "Static resource directory with path: '${config.path}' does not exist."
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."
        if (config.location == Location.CLASSPATH) {
            return Resource.newClassPathResource(config.path)?.toString() ?: throw RuntimeException("$noSuchDirMessage $classpathHint")
        }
        if (!File(config.path).exists()) {
            throw RuntimeException(noSuchDirMessage)
        }
        return config.path
    }

    override fun getResource(path: String): Resource {
        val targetResource by lazy { path.removePrefix(config.urlPathPrefix) }
        return when {
            config.urlPathPrefix == "/" -> super.getResource(path)!! // same as regular ResourceHandler
            targetResource == "" -> super.getResource("/")!! // directory without trailing '/'
            !path.startsWith(config.urlPathPrefix) -> EmptyResource.INSTANCE
            !targetResource.startsWith("/") -> EmptyResource.INSTANCE
            else -> super.getResource(targetResource)!!
        }
    }

}

private class AliasHandler(config: StaticFileConfig, private val aliasCheck: AliasCheck) : PrefixableHandler(config) {
    override fun getResource(path: String): Resource {  // if this method throws, we get a 404
        val resource = baseResource?.addPath(URIUtil.canonicalPath(path))!!
        if (!resource.isAlias) return super.getResource(path) // treat as prefixablehandler
        if (!aliasCheck.check(path, resource)) throw AccessDeniedException("Failed alias check")
        return resource // passed check, return the resource
    }
}
