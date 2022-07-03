package io.javalin.core.config

import io.javalin.http.Handler
import io.javalin.http.staticfiles.Location

class SinglePageConfig(private val pvt: PrivateConfig) {

    @JvmOverloads
    fun addRootFile(hostedPath: String, filePath: String, location: Location = Location.CLASSPATH) {
        pvt.singlePageHandler.add(hostedPath, filePath, location)
    }

    fun addRootHandler(hostedPath: String, customHandler: Handler) {
        pvt.singlePageHandler.add(hostedPath, customHandler)
    }

}
