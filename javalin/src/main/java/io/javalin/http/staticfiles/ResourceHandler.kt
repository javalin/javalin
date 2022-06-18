package io.javalin.http.staticfiles

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

enum class Location { CLASSPATH, EXTERNAL; }

interface ResourceHandler {
    fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean
    fun addStaticFileConfig(config: StaticFileConfig)
}
