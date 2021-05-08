/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.staticfiles

import io.javalin.core.util.JavalinLogger
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

class JettyResourceHandler : JavalinResourceHandler {

    val handlers = mutableListOf<ConfigurableHandler>()

    override fun addStaticFileConfig(config: StaticFileConfig) {
        handlers.add(ConfigurableHandler(config).apply { start() })
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
                    JavalinLogger.info("Exception occurred while handling static resource", e)
                }
            }
        }
        return false
    }

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory

    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
            this != null && this.isDirectory && handler.getResource("${target.removeSuffix("/")}/index.html")?.exists() == true
}

open class ConfigurableHandler(val config: StaticFileConfig) : ResourceHandler() {

    init {
        resourceBase = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        JavalinLogger.info("""Static file handler added:
        |    {hostedPath: "${config.hostedPath}", directory: "${config.directory}", location: Location.${config.location}}
        |    Resolved path: '${getResourceBase(config)}'
        """.trimMargin())
    }

    override fun getResource(path: String): Resource {
        val aliasResource by lazy { baseResource!!.addPath(URIUtil.canonicalPath(path)) }
        return when {
            config.directory == "META-INF/resources/webjars" ->
                Resource.newClassPathResource("META-INF/resources$path")
            config.aliasCheck != null && aliasResource.isAlias ->
                if (config.aliasCheck.check(path, aliasResource)) aliasResource else throw AccessDeniedException("Failed alias check")
            config.hostedPath == "/" -> super.getResource(path) // same as regular ResourceHandler
            path.startsWith(config.hostedPath) -> super.getResource(path.removePrefix(config.hostedPath))
            else -> EmptyResource.INSTANCE // files that don't start with hostedPath should not be accessible
        }
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

}
