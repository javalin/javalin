package io.javalin.http.staticfiles

import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
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
        // Security: ensure resolved path is still under base path
        if (!resolved.startsWith(basePath)) {
            return null
        }
        return FileSystemResource(resolved, basePath)
    }
    override fun isAlias(): Boolean {
        if (!exists()) return false
        // Check if any component in the path is a symlink by comparing normalized vs real path
        val normalizedPath = path.toAbsolutePath().normalize()
        val realPath = path.toRealPath()
        return normalizedPath != realPath
    }
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
            val url = classLoader.getResource(normalizedPath)
            return ClasspathResource(url, normalizedPath, normalizedPath, classLoader)
        }

        /** Normalize path by resolving . and .. segments using Path.normalize() */
        private fun normalizePath(path: String): String {
            if (path.isEmpty()) return path
            return Path.of(path).normalize().toString().replace('\\', '/')
        }
    }

    override fun exists(): Boolean = url != null

    override fun isDirectory(): Boolean {
        if (url == null) return false
        return when (url.protocol) {
            "file" -> Path.of(url.toURI()).isDirectory()
            "jar" -> resourcePath.endsWith("/") || classLoader.getResource("$resourcePath/") != null
            else -> false
        }
    }

    override fun length(): Long {
        if (url == null) return 0
        return try {
            url.openConnection().also { it.useCaches = false }.contentLengthLong
        } catch (e: Exception) {
            0
        }
    }

    override fun lastModified(): Long {
        if (url == null) return 0
        return try {
            url.openConnection().also { it.useCaches = false }.lastModified
        } catch (e: Exception) {
            0
        }
    }

    override fun fileName(): String? = resourcePath.substringAfterLast('/').takeIf { it.isNotEmpty() }

    override fun newInputStream(): InputStream = url?.openStream() ?: throw IllegalStateException("Resource not found: $resourcePath")

    override fun resolve(subPath: String): StaticResource? {
        val combinedPath = if (resourcePath.isEmpty()) {
            subPath.removePrefix("/")
        } else {
            "$resourcePath/${subPath.removePrefix("/")}".replace("//", "/")
        }
        val normalizedPath = normalizePath(combinedPath)
        // Security: ensure normalized path is still under base path
        if (!normalizedPath.startsWith(basePath) && basePath.isNotEmpty()) {
            return null
        }
        val url = classLoader.getResource(normalizedPath)
        return ClasspathResource(url, normalizedPath, basePath, classLoader)
    }

    override fun isAlias(): Boolean {
        if (url == null) return false
        return when (url.protocol) {
            "file" -> {
                // Check if any component in the path is a symlink by comparing normalized vs real path
                val path = Path.of(url.toURI())
                val normalizedPath = path.toAbsolutePath().normalize()
                val realPath = path.toRealPath()
                normalizedPath != realPath
            }
            else -> false
        }
    }

    override fun realPath(): Path? {
        if (url == null) return null
        return when (url.protocol) {
            "file" -> Path.of(url.toURI()).toRealPath()
            else -> null
        }
    }

    override fun toString(): String = url?.toString() ?: "ClasspathResource($resourcePath, not found)"
}

