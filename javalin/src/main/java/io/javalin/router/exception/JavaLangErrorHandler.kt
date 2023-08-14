package io.javalin.router.exception

import jakarta.servlet.http.HttpServletResponse

fun interface JavaLangErrorHandler {
    fun handle(res: HttpServletResponse, err: Error)
}
