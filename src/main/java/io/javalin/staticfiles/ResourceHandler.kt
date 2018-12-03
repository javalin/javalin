package io.javalin.staticfiles

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface ResourceHandler {
    fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse): Boolean
}
