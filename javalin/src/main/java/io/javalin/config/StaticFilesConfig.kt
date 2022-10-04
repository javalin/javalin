package io.javalin.config

import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.jetty.JettyResourceHandler
import java.util.function.Consumer

class StaticFilesConfig(private val pvt: PrivateConfig) {

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
        if (pvt.resourceHandler == null) {
            pvt.resourceHandler = JettyResourceHandler(pvt)
        }
        val finalConfig = StaticFileConfig()
        userConfig.accept(finalConfig)
        pvt.resourceHandler!!.addStaticFileConfig(finalConfig)
    }

}
