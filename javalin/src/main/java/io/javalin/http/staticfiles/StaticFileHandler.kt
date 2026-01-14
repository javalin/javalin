package io.javalin.http.staticfiles

import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.util.JavalinException
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.util.resource.ResourceFactory
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists

class StaticFileHandler(val config: StaticFileConfig) {

    val baseResource: StaticResource = getResourceBase()
    private val jettyResourceFactory = ResourceFactory.root()
    private val mimeTypes = MimeTypes.DEFAULTS

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
        // Treat trailing slash as an alias (e.g., /file/ -> /file)
        // This matches Jetty's behavior where non-canonical paths are aliases
        val isTrailingSlashAlias = path.endsWith("/") && !resolved.isDirectory()
        if (resolved.isAlias() || isTrailingSlashAlias) {
            val realPath = resolved.realPath() ?: return null
            val jettyResource = jettyResourceFactory.newResource(realPath)
            if (config.aliasCheck?.checkAlias(path, jettyResource) != true) {
                return null
            }
        }
        return resolved
    }

    fun resolveContentType(resource: StaticResource, fallbackPath: String): String? {
        val resourceName = resource.fileName() ?: fallbackPath
        val extension = resourceName.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it != resourceName }
        return extension?.let { ext ->
            config.mimeTypes.mapping()[ext.lowercase()] ?: mimeTypes.getMimeByExtension(resourceName)
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
        if (lastModified <= 0 && length <= 0) return null
        return "W/\"${java.lang.Long.toHexString(lastModified)}${java.lang.Long.toHexString(length)}\""
    }

    private fun getResourceBase(): StaticResource {
        val noSuchDirMsg: (String) -> String = { "Static resource directory with path: '$it' does not exist." }
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."

        if (config.location == Location.CLASSPATH) {
            val resource = ClasspathResource.create(Thread.currentThread().contextClassLoader, config.directory)
            if (!resource.exists()) {
                throw JavalinException("${noSuchDirMsg(config.directory)} $classpathHint")
            }
            return resource
        }

        val absolutePath = Path.of(config.directory).absolute().normalize()
        if (!absolutePath.exists()) {
            throw JavalinException(noSuchDirMsg(absolutePath.toString()))
        }
        return FileSystemResource(absolutePath)
    }
}

