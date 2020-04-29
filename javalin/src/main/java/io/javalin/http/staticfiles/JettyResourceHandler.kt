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
import io.javalin.http.LeveledBrotliStream
import io.javalin.http.LeveledGzipStream
import io.javalin.http.OutputStreamWrapper
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.resource.Resource
import java.io.File
import java.io.OutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyResourceHandler : io.javalin.http.staticfiles.ResourceHandler {

    val handlers = mutableListOf<ResourceHandler>()

    // It would work without a server, but if none is set jetty will log a warning.
    private val dummyServer = Server()

    override fun addStaticFileConfig(config: StaticFileConfig) {
        val handler = if (config.path == "/webjars") WebjarHandler() else StaticFileHandler().apply {
            resourceBase = getResourcePath(config)
            isDirAllowed = false
            isEtags = true
            enforceContentLengthHeader = config.enforceContentLengthHeader
            Javalin.log?.info("Static file handler added with path=${config.path} and location=${config.location}. Absolute path: '${getResourcePath(config)}'.")
        }
        handlers.add(handler.apply {
            server = dummyServer
            start()
        })
    }

    enum class CompressType(val type: String, val extension: String) {
        GZIP("gzip", ".gz"),
        BR("br", ".br")
    }

    val tempDir = createTempDir("javalin-compress").also { it.deleteOnExit() }

    companion object {
        val excludedMimeTypes = setOf(
                "image/",
                "audio/",
                "video/",
                "application/compress",
                "application/zip",
                "application/gzip",
                "application/bzip2",
                "application/brotli",
                "application/x-xz",
                "application/x-rar-compressed")
    }

    inner class WebjarHandler : ResourceHandler() {
        override fun getResource(path: String) = Resource.newClassPathResource("META-INF/resources$path")
                ?: super.getResource(path)
    }

    inner class StaticFileHandler : ResourceHandler() {
        var enforceContentLengthHeader = false
        val CUSTOM_COMPRESS_PREFIX = "javalin-pecompressed-files/"
        fun handleFile(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
            var resource = getResource(target)
            if(resource.isDirectoryWithWelcomeFile(this, target)) return

            response.setHeader(Header.ETAG, resource.weakETag)
            response.setHeader(Header.ACCEPT_RANGES, "bytes") //all resource on jetty can accept range
            response.setDateHeader(Header.LAST_MODIFIED, resource.lastModified())
            response.setHeader(Header.CONTENT_TYPE, MimeTypes.getDefaultMimeByExtension(target))
            val rwc = (response as JavalinResponseWrapper).rwc
            var compressed = false
            if(request.getHeader(Header.RANGE).isNullOrEmpty()) {
                if (!excludedMimeType(response.contentType ?: "")
                        && resource.length() > OutputStreamWrapper.minSizeForCompression) {
                    when {
                        rwc.acceptsBrotli && rwc.compStrat.brotli != null -> {
                            resource = getCompressedFile(resource, target, CompressType.BR, rwc.compStrat.brotli.level)
                            response.setHeader(Header.CONTENT_ENCODING, CompressType.BR.type)
                            compressed = true
                        }
                        rwc.acceptsGzip && rwc.compStrat.gzip != null -> {
                            resource = getCompressedFile(resource, target, CompressType.GZIP, rwc.compStrat.gzip.level)
                            response.setHeader(Header.CONTENT_ENCODING, CompressType.GZIP.type)
                            compressed = true
                        }
                    }
                }
                if (enforceContentLengthHeader) {
                    //set content-length before write response
                    response.setContentLengthLong(resource.length())
                }
                if(compressed){
                    response.write(resource.inputStream)
                    baseRequest.isHandled = true
                    return
                }
            }
            response.contentType = null
            super.handle(target, baseRequest, request, response)
        }

        override fun getResource(path: String): Resource {// return custom resource
            if (path.startsWith(CUSTOM_COMPRESS_PREFIX)) {
                return Resource.newResource(path.substringAfter(CUSTOM_COMPRESS_PREFIX))
            }
            return super.getResource(path)
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

    private fun getCompressedFile(originResource: Resource, target: String, type: CompressType, level: Int): Resource {
        val tmp = File(tempDir.path, target + type.extension).apply {
            this.parentFile.also {
                it.mkdirs()
                it.deleteOnExit()
            }
            this.deleteOnExit()
        }
        if (!tmp.exists()) {
            val fileInput = originResource.inputStream
            val outputStream: OutputStream = when (type) {
                CompressType.GZIP -> {
                    LeveledGzipStream(tmp.outputStream(), level)
                }
                CompressType.BR -> {
                    LeveledBrotliStream(tmp.outputStream(), level)
                }
            }
            val buffer = ByteArray(2048)
            var bytesRead: Int
            while (fileInput.read(buffer).also { bytesRead = it } > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }
            fileInput.close()
            outputStream.close()
        }
        return Resource.newResource(tmp)
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
                    httpResponse.contentType = null
                    if (handler is StaticFileHandler)
                        handler.handleFile(target, baseRequest, httpRequest, httpResponse)
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

    private fun excludedMimeType(mimeType: String) =
            if (mimeType == "") false else excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory

    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String) =
            this != null && this.isDirectory && handler.getResource("${target.removeSuffix("/")}/index.html")?.exists() == true

}
