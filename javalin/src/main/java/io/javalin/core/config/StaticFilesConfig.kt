package io.javalin.core.config

import io.javalin.core.util.Header
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.jetty.JettyResourceHandler
import java.util.function.Consumer

class StaticFilesConfig(private val inner: InnerConfig) {

    fun enableWebjars() = add { staticFiles ->
        staticFiles.directory = "META-INF/resources/webjars"
        staticFiles.headers = mapOf(Header.CACHE_CONTROL to "max-age=31622400")
    }

    @JvmOverloads
    fun add(directory: String, location: Location = Location.CLASSPATH) = add { staticFiles ->
        staticFiles.directory = directory
        staticFiles.location = location
    }

    fun add(userConfig: Consumer<StaticFileConfig>) {
        if (inner.resourceHandler == null) {
            inner.resourceHandler = JettyResourceHandler()
        }
        val finalConfig = StaticFileConfig()
        userConfig.accept(finalConfig)
        inner.resourceHandler!!.addStaticFileConfig(finalConfig)
    }

}
