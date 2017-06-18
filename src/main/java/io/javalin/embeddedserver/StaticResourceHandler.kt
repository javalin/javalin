/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface StaticResourceHandler {
    // should return if request has been handled
    fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean
}

data class StaticFileConfig(val path: String, val location: Location)

enum class Location {
    CLASSPATH, EXTERNAL;
}
