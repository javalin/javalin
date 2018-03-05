/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Serves static resources based on supplied configuration.
 *
 * @see StaticFileConfig
 */
interface StaticResourceHandler {
    /**
     * Handles static file requests.
     *
     * @return whether the request has been handled.
     */
    fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean
}

/**
 * Configuration for serving static files.
 */
data class StaticFileConfig(val path: String, val location: Location)

enum class Location {
    CLASSPATH, EXTERNAL;
}
