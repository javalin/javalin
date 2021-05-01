/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.staticfiles

import io.javalin.Javalin
import io.javalin.core.util.Util
import io.javalin.http.JavalinResponseWrapper
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.resource.EmptyResource
import org.eclipse.jetty.util.resource.Resource
import java.io.File
import java.nio.file.AccessDeniedException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import io.javalin.http.staticfiles.ResourceHandler as JavalinResourceHandler

open class ConfigResourceHandler(val config: StaticFileConfig) : ResourceHandler()

class JettyResourceHandler : JavalinResourceHandler {

    val handlers = mutableListOf<ConfigResourceHandler>()

    override fun addStaticFileConfig(config: StaticFileConfig) {
        handlers.add(when {
            config.directory == "/webjars" -> WebjarHandler(config)
            config.aliasCheck != null -> AliasHandler(config)
            else -> PrefixableHandler(config)
        }.apply { start() })
    }

    override fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean {
        val target = httpRequest.getAttribute("jetty-target") as String
        val baseRequest = httpRequest.getAttribute("jetty-request") as Request
        for (handler in handlers) {
            try {
                val resource = handler.getResource(target)
                if (resource.isFile() || resource.isDirectoryWithWelcomeFile(handler, target)) {
                    handler.config.headers.forEach { httpResponse.setHeader(it.key, it.value) }
                    if (handler.config.precompress && PrecompressingResourceHandler.handle(resource, httpRequest, httpResponse)) {
                        return true
                    }
                    httpResponse.contentType = null // Jetty will only set the content-type if it's null
                    handler.handle(target, baseRequest, httpRequest, httpResponse)
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

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory

    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
            this != null && this.isDirectory && handler.getResource("${target.removeSuffix("/")}/index.html")?.exists() == true
}

private class WebjarHandler(config: StaticFileConfig) : ConfigResourceHandler(config) {
    override fun getResource(path: String) = Resource.newClassPathResource("META-INF/resources$path") ?: super.getResource(path)
}

private open class PrefixableHandler(config: StaticFileConfig) : ConfigResourceHandler(config) {

    init {
        resourceBase = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        Javalin.log?.info("""Static file handler added:
        |    {urlPathPrefix: "${config.urlPathPrefix}", path: "${config.directory}", location: Location.${config.location}}
        |    Resolved path: '${getResourceBase(config)}'
        """.trimMargin())
    }

    private fun getResourceBase(config: StaticFileConfig): String {
        val noSuchDirMessage = "Static resource directory with path: '${config.directory}' does not exist."
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."
        if (config.location == Location.CLASSPATH) {
            return Resource.newClassPathResource(config.directory)?.toString() ?: throw RuntimeException("$noSuchDirMessage $classpathHint")
        }
        if (!File(config.directory).exists()) {
            throw RuntimeException(noSuchDirMessage)
        }
        return config.directory
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

private class AliasHandler(config: StaticFileConfig) : PrefixableHandler(config) {
    override fun getResource(path: String): Resource {  // if this method throws, we get a 404
        val resource = baseResource?.addPath(URIUtil.canonicalPath(path))!!
        if (!resource.isAlias) return super.getResource(path) // treat as prefixablehandler
        if (!config.aliasCheck!!.check(path, resource)) throw AccessDeniedException("Failed alias check")
        return resource // passed check, return the resource
    }
}
