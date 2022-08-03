package io.javalin.http.staticfiles

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
) {
    internal fun refinedToString(): String {
        return this.toString().replace(", skipFileFunction=(jakarta.servlet.http.HttpServletRequest) -> kotlin.Boolean", "")
    }
}
