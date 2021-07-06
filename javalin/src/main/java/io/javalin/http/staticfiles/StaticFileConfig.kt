package io.javalin.http.staticfiles

import io.javalin.core.util.Header
import org.eclipse.jetty.server.handler.ContextHandler.AliasCheck
import java.util.*

data class StaticFileConfig(
    @JvmField var hostedPath: String = "/",
    @JvmField var directory: String = "/public",
    @JvmField var location: Location = Location.CLASSPATH,
    @JvmField var precompress: Boolean = false,
    @JvmField var aliasCheck: AliasCheck? = null,
    @JvmField var headers: Map<String, String> = HashMap(Collections.singletonMap(Header.CACHE_CONTROL, "max-age=0"))
)
