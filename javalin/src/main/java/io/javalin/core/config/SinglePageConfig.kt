package io.javalin.core.config

import io.javalin.http.Handler
import io.javalin.http.staticfiles.Location

class SinglePageConfig(private val inner: InnerConfig) {

    @JvmOverloads
    fun addRootFile(hostedPath: String, filePath: String, location: Location = Location.CLASSPATH) {
        inner.singlePageHandler.add(hostedPath, filePath, location)
    }

    fun addRootHandler(hostedPath: String, customHandler: Handler) {
        inner.singlePageHandler.add(hostedPath, customHandler)
    }

}
