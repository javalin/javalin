package io.javalin.http.staticfiles

import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * Abstraction for static resources (classpath or filesystem).
 * Replaces org.eclipse.jetty.util.resource.Resource.
 */
interface StaticResource {
    fun exists(): Boolean
    fun isDirectory(): Boolean
    fun length(): Long
    fun lastModified(): Long
    fun fileName(): String?
    fun newInputStream(): InputStream
    fun resolve(subPath: String): StaticResource?
    fun isAlias(): Boolean
    fun realPath(): Path?
}

class FileSystemResource(private val path: Path, private val basePath: Path = path) : StaticResource {
    override fun exists(): Boolean = path.exists()
    override fun isDirectory(): Boolean = path.isDirectory()
    override fun length(): Long = if (exists() && !isDirectory()) Files.size(path) else 0
    override fun lastModified(): Long = if (exists()) Files.getLastModifiedTime(path).toMillis() else 0
    override fun fileName(): String? = path.name.takeIf { it.isNotEmpty() }
    override fun newInputStream(): InputStream = Files.newInputStream(path)
    override fun resolve(subPath: String): StaticResource? {
        val resolved = path.resolve(subPath.removePrefix("/")).normalize()
        if (!resolved.startsWith(basePath)) return null // Security: stay under base path
        return FileSystemResource(resolved, basePath)
    }
    override fun isAlias(): Boolean = exists() && path.toAbsolutePath().normalize() != path.toRealPath()
    override fun realPath(): Path? = if (exists()) path.toRealPath() else null
    override fun toString(): String = path.toString()
}

class ClasspathResource private constructor(
    private val url: URL?,
    private val resourcePath: String,
    private val basePath: String,
    private val classLoader: ClassLoader
) : StaticResource {

    companion object {
        fun create(classLoader: ClassLoader, resourcePath: String): ClasspathResource {
            val normalizedPath = normalizePath(resourcePath.removePrefix("/"))
            return ClasspathResource(classLoader.getResource(normalizedPath), normalizedPath, normalizedPath, classLoader)
        }

        private fun normalizePath(path: String): String =
            if (path.isEmpty()) path else Path.of(path).normalize().toString().replace('\\', '/')
    }

    private fun <T> urlConnection(getter: java.net.URLConnection.() -> T): T? = try {
        url?.openConnection()?.also { it.useCaches = false }?.getter()
    } catch (e: Exception) { null }

    override fun exists(): Boolean = url != null
    override fun length(): Long = urlConnection { contentLengthLong } ?: 0
    override fun lastModified(): Long = urlConnection { lastModified } ?: 0
    override fun fileName(): String? = resourcePath.substringAfterLast('/').takeIf { it.isNotEmpty() }
    override fun newInputStream(): InputStream = url?.openStream() ?: throw IllegalStateException("Resource not found: $resourcePath")

    override fun isDirectory(): Boolean = when (url?.protocol) {
        "file" -> Path.of(url.toURI()).isDirectory()
        "jar" -> resourcePath.endsWith("/") || classLoader.getResource("$resourcePath/") != null
        else -> false
    }

    override fun resolve(subPath: String): StaticResource? {
        val normalizedPath = normalizePath("$resourcePath/${subPath.removePrefix("/")}")
        if (basePath.isNotEmpty() && !normalizedPath.startsWith(basePath)) return null // Security: stay under base path
        return ClasspathResource(classLoader.getResource(normalizedPath), normalizedPath, basePath, classLoader)
    }

    override fun isAlias(): Boolean = when (url?.protocol) {
        "file" -> Path.of(url.toURI()).let { it.toAbsolutePath().normalize() != it.toRealPath() }
        else -> false
    }

    override fun realPath(): Path? = when (url?.protocol) {
        "file" -> Path.of(url.toURI()).toRealPath()
        else -> null
    }

    override fun toString(): String = url?.toString() ?: "ClasspathResource($resourcePath, not found)"
}

