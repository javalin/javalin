package io.javalin.http.staticfiles

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

enum class Location { CLASSPATH, EXTERNAL; }
data class StaticFileConfig(val path: String, val location: Location, var enforceContentLengthHeader: Boolean)

interface ResourceHandler {
    fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean
    fun addStaticFileConfig(config: StaticFileConfig)
}
