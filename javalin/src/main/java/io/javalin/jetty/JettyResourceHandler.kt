/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.PrivateConfig
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.resource.EmptyResource
import org.eclipse.jetty.util.resource.Resource
import java.io.File
import java.nio.file.AccessDeniedException
import io.javalin.http.staticfiles.ResourceHandler as JavalinResourceHandler

class JettyResourceHandler(val pvt: PrivateConfig) : JavalinResourceHandler {

    fun init() { // we delay the creation of ConfigurableHandler objects to get our logs in order during startup
        handlers.addAll(lateInitConfigs.map { ConfigurableHandler(it, pvt.server!!) })
    }

    private val lateInitConfigs = mutableListOf<StaticFileConfig>()
    private val handlers = mutableListOf<ConfigurableHandler>()

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean =
        if (pvt.server?.isStarted == true) handlers.add(ConfigurableHandler(config, pvt.server!!)) else lateInitConfigs.add(config)

    override fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean {
        val (target, baseRequest) = httpRequest.getAttribute("jetty-target-and-request") as Pair<String, Request>
        handlers.filter { !it.config.skipFileFunction(httpRequest) }.forEach { handler ->
            try {
                val resource = handler.getResource(target)
                if (resource.isFile() || resource.isDirectoryWithWelcomeFile(handler, target)) {
                    handler.config.headers.forEach { httpResponse.setHeader(it.key, it.value) }
                    if (handler.config.precompress) {
                        return if (resource.isDirectoryWithWelcomeFile(handler, target))  // if it's a directory, we need to serve the welcome file
                            JettyPrecompressingResourceHandler.handle(target, getWelcomeFile(handler, target), httpRequest, httpResponse)
                        else
                            JettyPrecompressingResourceHandler.handle(target, resource, httpRequest, httpResponse)
                    }
                    httpResponse.contentType = null // Jetty will only set the content-type if it's null
                    return runCatching { handler.handle(target, baseRequest, httpRequest, httpResponse) }.isSuccess
                }
            } catch (e: Exception) { // it's fine, we'll just 404
                if (!JettyUtil.isClientAbortException(e)) {
                    JavalinLogger.info("Exception occurred while handling static resource", e)
                }
            }
        }
        return false
    }

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory
    private fun getWelcomeFile(handler: ResourceHandler, target: String) =
        handler.getResource("${target.removeSuffix("/")}/index.html")

    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
        this != null && this.isDirectory && getWelcomeFile(handler, target)?.exists() == true

}

open class ConfigurableHandler(val config: StaticFileConfig, jettyServer: Server) : ResourceHandler() {

    init {
        JavalinLogger.info("Static file handler added: ${config.refinedToString()}. File system location: '${getResourceBase(config)}'")
        resourceBase = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        server = jettyServer
        mimeTypes = MimeTypes()
        config.mimeTypes.getMapping().forEach { (ext, mimeType) ->
            mimeTypes.addMimeMapping(ext, mimeType)
        }
        start()
    }

    override fun getResource(path: String): Resource {
        val aliasResource by lazy { baseResource!!.addPath(URIUtil.canonicalPath(path)) }
        return when {
            config.directory == "META-INF/resources/webjars" ->
                Resource.newClassPathResource("META-INF/resources$path") ?: EmptyResource.INSTANCE

            config.aliasCheck != null && aliasResource.isAlias ->
                if (config.aliasCheck?.check(path, aliasResource) == true) aliasResource else throw AccessDeniedException("Failed alias check")

            config.hostedPath == "/" -> super.getResource(path) // same as regular ResourceHandler
            path.startsWith(config.hostedPath + "/") -> super.getResource(path.removePrefix(config.hostedPath))
            else -> EmptyResource.INSTANCE // files that don't start with hostedPath should not be accessible
        }
    }

    private fun getResourceBase(config: StaticFileConfig): String {
        val noSuchDirMessage = "Static resource directory with path: '${config.directory}' does not exist."
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."
        if (config.location == Location.CLASSPATH) {
            return Resource.newClassPathResource(config.directory)?.toString() ?: throw JavalinException("$noSuchDirMessage $classpathHint")
        }
        if (!File(config.directory).exists()) {
            throw JavalinException(noSuchDirMessage)
        }
        return config.directory
    }

}
