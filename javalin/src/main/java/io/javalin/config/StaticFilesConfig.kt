package io.javalin.config

import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.http.staticfiles.NativeResourceHandler
import io.javalin.security.RouteRole
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
        staticFiles.hostedPath = "/webjars"
        staticFiles.directory = "META-INF/resources/webjars"
        staticFiles.headers = mapOf(Header.CACHE_CONTROL to "max-age=31622400")
    }

    /**
     * Adds the given directory as a static file containers.
     * @param directory the directory where your files are located
     * @param location the location of the static directory (default: CLASSPATH)
     * @param roles the roles which can access the the static directory (default: emptySet())
     */
    @JvmOverloads
    fun add(directory: String, location: Location = Location.CLASSPATH, roles: Set<RouteRole> = emptySet()) = add { staticFiles ->
        staticFiles.directory = directory
        staticFiles.location = location
        staticFiles.roles = roles
    }

    /**
     * Adds a static file through custom configuration.
     * @param userConfig a lambda to configure advanced static files
     */
    fun add(userConfig: Consumer<StaticFileConfig>) {
        if (cfg.pvt.resourceHandler == null) {
            cfg.pvt.resourceHandler = NativeResourceHandler(cfg.pvt)
        }
        val finalConfig = StaticFileConfig()
        userConfig.accept(finalConfig)
        cfg.pvt.resourceHandler!!.addStaticFileConfig(finalConfig)
    }

}
