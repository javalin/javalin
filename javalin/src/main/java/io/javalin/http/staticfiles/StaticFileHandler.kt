package io.javalin.http.staticfiles

import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.util.JavalinException
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists

class StaticFileHandler(val config: StaticFileConfig) {

    val baseResource: StaticResource = getResourceBase()

    fun handleResource(resourcePath: String, ctx: Context): Boolean {
        val resource = getResource(resourcePath) ?: return false
        resolveContentType(resource, resourcePath)?.let { ctx.contentType(it) }
        if (tryHandleEtag(resource, ctx)) return true
        resource.newInputStream().use { ctx.result(it.readAllBytes()) }
        return true
    }

    fun getResource(path: String): StaticResource? = runCatching {
        baseResource.takeIfValid(path) ?: baseResource.takeIfValid("$path/index.html")
    }.getOrNull()

    private fun StaticResource.takeIfValid(path: String): StaticResource? {
        val resolved = this.resolve(path) ?: return null
        if (!resolved.exists() || resolved.isDirectory()) return null
        // Block symlinks and trailing slashes on files (e.g., /file/) unless aliasCheck allows
        if (resolved.isAlias() || path.endsWith("/")) {
            val realPath = resolved.realPath() ?: return null
            if (config.aliasCheck?.check(path, realPath) != true) return null
        }
        return resolved
    }

    fun resolveContentType(resource: StaticResource, fallbackPath: String): String? {
        val resourceName = resource.fileName() ?: fallbackPath
        val extension = resourceName.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it != resourceName }
        return extension?.let { ext ->
            config.mimeTypes.mapping()[ext.lowercase()] ?: ContentType.mimeTypeByExtension(ext)
        }
    }

    fun tryHandleEtag(resource: StaticResource, ctx: Context): Boolean {
        val etag = computeWeakEtag(resource) ?: return false
        if (ctx.header(Header.IF_NONE_MATCH) == etag) {
            ctx.status(304)
            return true
        }
        ctx.header(Header.ETAG, etag)
        return false
    }

    private fun computeWeakEtag(resource: StaticResource): String? {
        val lastModified = resource.lastModified()
        val length = resource.length()
        return if (lastModified <= 0 && length <= 0) null else "W/\"${lastModified.toString(16)}-${length.toString(16)}\""
    }

    private fun getResourceBase(): StaticResource {
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."
        return when (config.location) {
            Location.CLASSPATH -> {
                val resource = ClasspathResource.create(Thread.currentThread().contextClassLoader, config.directory)
                if (!resource.exists()) throw JavalinException("Static resource directory '${config.directory}' not found. $classpathHint")
                resource
            }
            Location.EXTERNAL -> {
                val path = Path.of(config.directory).absolute().normalize()
                if (!path.exists()) throw JavalinException("Static resource directory '$path' not found.")
                FileSystemResource(path)
            }
        }
    }
}

