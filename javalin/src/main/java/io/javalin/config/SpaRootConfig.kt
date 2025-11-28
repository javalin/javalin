package io.javalin.config

import io.javalin.http.Handler
import io.javalin.http.SinglePageHandler
import io.javalin.http.staticfiles.Location

/**
 * Configures a Single Page Application root.
 *
 * Single page application (SPA) mode is similar to static file handling.
 * It runs after endpoint matching, and after static file handling. It’s
 * basically a very fancy 404 mapper, which converts any 404’s into a
 * specified page. You can define multiple single page handlers for your
 * application by specifying different root paths.
 *
 * @see [SinglePageHandler]
 */
class SpaRootConfig(private val cfg: JavalinState) {

    @JvmOverloads
    fun addFile(hostedPath: String, filePath: String, location: Location = Location.CLASSPATH) {
        cfg.singlePageHandler.add(hostedPath, filePath, location)
    }

    fun addHandler(hostedPath: String, customHandler: Handler) {
        cfg.singlePageHandler.add(hostedPath, customHandler)
    }

}
