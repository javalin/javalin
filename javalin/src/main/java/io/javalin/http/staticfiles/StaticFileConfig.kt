package io.javalin.http.staticfiles

import io.javalin.http.ContentType
import io.javalin.http.Header
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.server.handler.ContextHandler.AliasCheck

enum class Location { CLASSPATH, EXTERNAL; }

data class StaticFileConfig(
    @JvmField var hostedPath: String = "/",
    @JvmField var directory: String = "/public",
    @JvmField var location: Location = Location.CLASSPATH,
    @JvmField var precompress: Boolean = false,
    @JvmField var aliasCheck: AliasCheck? = null,
    @JvmField var headers: Map<String, String> = mutableMapOf(Header.CACHE_CONTROL to "max-age=0"),
    @JvmField var skipFileFunction: (HttpServletRequest) -> Boolean = { false },
    @JvmField val mimeTypes: MimeTypesConfig = MimeTypesConfig()
) {
    internal fun refinedToString(): String {
        return this.toString().replace(", skipFileFunction=(jakarta.servlet.http.HttpServletRequest) -> kotlin.Boolean", "")
    }
}

class MimeTypesConfig {
    private val extensionToMimeType: MutableMap<String, String> = mutableMapOf()

    fun getMapping(): Map<String, String> = extensionToMimeType.toMap()

    fun add(contentType: ContentType) {
        add(contentType.mimeType, *contentType.extensions)
    }

    fun add(contentType: ContentType, vararg extensions: String) {
        add(contentType.mimeType, *extensions)
    }

    fun add(mimeType: String, vararg extensions: String) {
        extensions.forEach { ext ->
            extensionToMimeType[ext] = mimeType
        }
    }
}
