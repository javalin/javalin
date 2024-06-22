package io.javalin.config

import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.ResourceHandlerImpl
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.jetty.JettyResourceHandler
import java.util.function.Consumer

/**
 * Configuration for static files and webjars.
 *
 * Static resource handling is done after endpoint matching,
 * meaning your own GET endpoints have higher priority.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinConfig.staticFiles]
 */
class StaticFilesConfig(private val cfg: JavalinConfig) {

    /** Enable webjars access. They will be available at /webjars/name/version/file.ext. */
    fun enableWebjars() = add { staticFiles ->
        staticFiles.directory = "META-INF/resources/webjars"
        staticFiles.headers = mapOf(Header.CACHE_CONTROL to "max-age=31622400")
    }

    /**
     * Adds the given directory as a static file containers.
     * @param directory the directory where your files are located
     * @param location the location of the static directory (default: CLASSPATH)
     */
    @JvmOverloads
    fun add(directory: String, location: Location = Location.CLASSPATH) = add { staticFiles ->
        staticFiles.directory = directory
        staticFiles.location = location
    }

    /**
     * Adds a static file through custom configuration.
     * @param userConfig a lambda to configure advanced static files
     */
    fun add(userConfig: Consumer<StaticFileConfig>) {
        if (cfg.pvt.resourceHandler == null) {
            //cfg.pvt.resourceHandler = JettyResourceHandler(cfg.pvt)
            cfg.pvt.resourceHandler = ResourceHandlerImpl(cfg.pvt)
        }
        val finalConfig = StaticFileConfig()
        userConfig.accept(finalConfig)
        cfg.pvt.resourceHandler!!.addStaticFileConfig(finalConfig)
    }

}
