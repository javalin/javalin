package io.javalin.http.staticfiles

import io.javalin.config.StaticFilesConfig
import io.javalin.http.ContentType
import io.javalin.http.Header
import io.javalin.security.RouteRole
import jakarta.servlet.http.HttpServletRequest
import java.nio.file.Path

/** The static files location. */
enum class Location {
    /** Static resources are available in the classpath (jar) */
    CLASSPATH,

    /**
     * Static resources are external (on the file system)
     */
    EXTERNAL;
}

/**
 * Functional interface for checking if an alias (symlink) should be allowed.
 * Used to configure symlink handling for static files.
 */
fun interface AliasCheck {
    /**
     * Check if the alias should be allowed.
     * @param path the request path
     * @param realPath the real path that the symlink resolves to
     * @return true if the alias should be allowed, false otherwise
     */
    fun check(path: String, realPath: Path): Boolean
}

/**
 * The configuration for Static Files.
 * @param hostedPath change to host files on a subpath, like '/assets' (default: '/')
 * @param directory the directory where your files are located
 * @param location Location.CLASSPATH (jar) or Location.EXTERNAL (file system) (default: CLASSPATH)
 * @param precompressMaxSize if the files should be pre-compressed and cached in memory (default: -1, disabled)
 * @param aliasCheck can be used to configure SymLinks
 * @param headers headers that will be set for the static files
 * @param skipFileFunction lambda to skip certain files in the dir, based on the HttpServletRequest
 * @param mimeTypes configuration for file extension based Mime Types
 * @see [StaticFilesConfig]
 */
data class StaticFileConfig(
    @JvmField var hostedPath: String = "/",
    @JvmField var directory: String = "/public",
    @JvmField var location: Location = Location.CLASSPATH,
    @JvmField var precompressMaxSize: Int = -1, // -1 means disabled, otherwise set the max size for pre-compression
    @JvmField var aliasCheck: AliasCheck? = null,
    @JvmField var headers: Map<String, String> = mutableMapOf(Header.CACHE_CONTROL to "max-age=0"),
    @JvmField var skipFileFunction: ((HttpServletRequest) -> Boolean)? = null,
    @JvmField val mimeTypes: MimeTypesConfig = MimeTypesConfig(),
    @JvmField var roles: Set<RouteRole> = emptySet()
)

/** Configures static files Mime Types based on file extensions.*/
class MimeTypesConfig {
    private val extensionToMimeType: MutableMap<String, String> = mutableMapOf()

    fun mapping(): Map<String, String> = extensionToMimeType.toMap()

    /**
     * Adds a known content type to this configuration.
     * @param contentType the content type to add
     */
    fun add(contentType: ContentType) {
        add(contentType.mimeType, *contentType.extensions)
    }

    /**
     * Adds a MimeType for custom file extensions.
     * @param contentType the content type to add
     * @param extensions the extensions to use the given content type
     */
    fun add(contentType: ContentType, vararg extensions: String) {
        add(contentType.mimeType, *extensions)
    }


    /**
     * Adds a MimeType for custom file extensions.
     * @param mimeType the mime type to add
     * @param extensions the extensions to use the given mime type
     */
    fun add(mimeType: String, vararg extensions: String) {
        extensions.forEach { ext ->
            extensionToMimeType[ext] = mimeType
        }
    }

    override fun toString(): String = extensionToMimeType.toString()
}
