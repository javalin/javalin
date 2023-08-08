package io.javalin.config

import io.javalin.http.Handler
import io.javalin.http.staticfiles.Location

class SpaRootConfig(private val cfg: JavalinConfig) {

    @JvmOverloads
    fun addFile(hostedPath: String, filePath: String, location: Location = Location.CLASSPATH) {
        cfg.pvt.singlePageHandler.add(hostedPath, filePath, location)
    }

    fun addHandler(hostedPath: String, customHandler: Handler) {
        cfg.pvt.singlePageHandler.add(hostedPath, customHandler)
    }

}
