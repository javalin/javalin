package io.javalin.http.staticfiles

import io.javalin.config.PrivateConfig
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.io.EofException
import org.eclipse.jetty.util.resource.Resource
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import kotlin.io.path.Path
import kotlin.io.path.absolute

class ResourceHandlerImpl(val pvt: PrivateConfig) : ResourceHandler {

    fun init() { // we delay the creation of ConfigurableHandler objects to get our logs in order during startup
        handlers.addAll(staticFileConfigs.map { StaticFileHandler(it) })
    }
    private val staticFileConfigs = mutableListOf<StaticFileConfig>()
    private val handlers = mutableListOf<StaticFileHandler>()

    override fun canHandle(ctx: Context): Boolean = nonSkippedHandlers(ctx.req()).any { handler ->
        return try {
            fileOrWelcomeFile(handler, ctx.path()) != null
        } catch (e: Exception) {
            e.message?.contains("Rejected alias reference") == true ||  // we want to say these are un-handleable (404)
                e.message?.contains("Failed alias check") == true || // we want to say these are un-handleable (404)
                e.message?.contains("Path is outside of the base directory") == true // we want to say these are un-handleable (404)

        }
    }

    override fun handle(ctx: Context): Boolean {
        nonSkippedHandlers(ctx.req()).forEach { handler ->
            try {
                val target = ctx.target
                val fileOrWelcomeFile = fileOrWelcomeFile(handler, target)
                if (fileOrWelcomeFile != null) {
                    handler.config.headers.forEach { ctx.header(it.key, it.value) } // set user headers
                    return when (handler.config.precompress) {
                        true -> PrecompressingResourceHandler.handle(target, fileOrWelcomeFile, ctx, pvt.compressionStrategy)
                        false -> {
                            runCatching { // we wrap the response to compress it with javalin's compression strategy
                                handler.handle(target, fileOrWelcomeFile ,ctx)
                            }.isSuccess
                        }
                    }
                }
            } catch (e: Exception) { // it's fine, we'll just 404
                if (e !is EofException) { // EofException is thrown when the client disconnects, which is fine
                    JavalinLogger.info("Exception occurred while handling static resource", e)
                }
            }
        }
        return false
    }

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean = staticFileConfigs.add(config)

    private fun nonSkippedHandlers(servletRequest: HttpServletRequest) =
        handlers.asSequence().filter { !it.config.skipFileFunction(servletRequest) }

    private val Context.target get() = this.req().requestURI.removePrefix(this.req().contextPath)

    private fun fileOrWelcomeFile(handler: StaticFileHandler, target: String): StaticResource? =
        handler.getResource(target) ?: handler.getResource("${target.removeSuffix("/")}/index.html")


}

private class StaticFileHandler(val config: StaticFileConfig) {

    init {
        JavalinLogger.info("Static file handler added: ${config.refinedToString()}. File system location: '${getResourceBase(config)}'")
        ContentType.entries.forEach { config.mimeTypes.add(it) } // add default mime types to user-configured mime types
    }

    fun getResource(target: String): StaticResource? {
        return when {
            target.startsWith(config.hostedPath).not() -> null // we don't want to serve files outside the directory
            target.endsWith("/") -> null // we don't want to serve directories
            config.directory == "META-INF/resources/webjars" -> {
                val resource = javaClass.classLoader.getResource("META-INF/resources$target")
                return resource?.let { jarFileResource(it) }
            }
            //TODO: Missing alias check previously in JettyResourceHandler, check how to implement it
            config.location == Location.CLASSPATH -> {
                val resource = javaClass.classLoader.getResource("${config.directory.removeSuffix("/").removePrefix("/")}/${target.removePrefix(config.hostedPath)}")
                // Test if the resource exists and is not a directory
                return resource?.let {
                    File(it.toURI()).takeIf { it.exists() && !it.isDirectory }?.let { FileResource(it) }
                }
            }
            config.location == Location.EXTERNAL -> {
                val sanitizedPath = sanitizePath(target) ?: return null // we don't want to serve files outside the directory
                return File(sanitizedPath).takeIf { it.exists() && !it.isDirectory }?.let { FileResource(it) }
            }
            else -> null
        }
    }

    fun sanitizePath(target: String): String? {
        val normalized = Paths.get(target).normalize().toString()
        // we don't want to serve files outside the directory
        // but we must also support relative paths when adding external files
        val file = File(config.directory, normalized)
        return if (file.canonicalPath.contains(config.directory)) { // canonicalPath is absolute, so we can use it to check if the file is outside the directory
            file.canonicalPath
        } else {
            null
        }
    }

    /**
     * Load a file contained in a webjar
     */
    fun jarFileResource(path: URL): StaticResource? {
        try {
        val uri = path.toURI()
        val jarFile = JarFile(uri.schemeSpecificPart.substringBefore("!").substringAfter(":"))
        val entry = jarFile.getEntry(uri.schemeSpecificPart.substringAfter("!").removePrefix("/"))
        return JarFileResource(entry, jarFile)
        } catch (e: Exception) {
            JavalinLogger.info("Failed to load file from webjar", e)
            return null
        }
    }

    fun handle(target: String, file: StaticResource, ctx: Context) {
        config.headers.forEach { ctx.header(it.key, it.value) }
        // Add weak etag
        ctx.header(Header.IF_NONE_MATCH)?.let { requestEtag ->
            if (requestEtag == PrecompressingResourceHandler.generateWeakEtag(file)) { // jetty resource use weakETag too
                ctx.status(304)
                return true // return early if resource is same as client cached version
            }
        }
        val etag = Base64.getEncoder().encodeToString("${file.lastModified()}-${file.length()}".toByteArray())
        ctx.header("ETag", "W/\"$etag\"")
        ctx.outputStream().use { file.inputStream().copyTo(it) }
    }

    fun getMimeTypeForFile(target: String): String {
        val extension = target.substringAfterLast(".", "")
        return config.mimeTypes.getMapping()[extension] ?: ContentType.APPLICATION_OCTET_STREAM.mimeType
    }

    private fun getResourceBase(config: StaticFileConfig): String {
        val noSuchDirMessageBuilder: (String) -> String = { "Static resource directory with path: '$it' does not exist." }
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."
        if (config.location == Location.CLASSPATH) {
            return Resource.newClassPathResource(config.directory)?.toString() ?: throw JavalinException("${noSuchDirMessageBuilder(config.directory)} $classpathHint")
        }

        // Use the absolute path as this aids in debugging. Issues frequently come from incorrect root directories, not incorrect relative paths.
        val absoluteDirectoryPath = Path(config.directory).absolute().normalize()
        if (!Files.exists(absoluteDirectoryPath)) {
            throw JavalinException(noSuchDirMessageBuilder(absoluteDirectoryPath.toString()))
        }
        return config.directory
    }

}

/**
 * A single static file, that can come from the classpath, a webjar or the file system.
 */
interface StaticResource { //TODO: Missing implementation
    fun inputStream(): InputStream
    fun length(): Long
    fun lastModified(): Long = 0L //
}

class FileResource(private val file: File) : StaticResource {
    override fun inputStream(): InputStream = file.inputStream()
    override fun length(): Long = file.length()
    override fun lastModified(): Long = file.lastModified()
}

class JarFileResource(private val zipEntry: ZipEntry, private val jarFile: JarFile) : StaticResource {
    override fun inputStream(): InputStream = jarFile.getInputStream(zipEntry)
    override fun length(): Long = zipEntry.size
    override fun lastModified(): Long = zipEntry.lastModifiedTime.toMillis()
}
