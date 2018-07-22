/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.*

object HttpResponseExceptionMapper {

    fun map(e: HttpResponseException, ctx: Context) {
        if (ctx.header(Header.ACCEPT)?.contains("application/json") == true) {
            ctx.status(e.status).result("""{
                |    "title": "${e.msg}",
                |    "status": ${e.status},
                |    "type": "${getTypeUrl(e)}",
                |    "details": ${e.details.map { """{"${it.key}": "${it.value}"}""" }}
                |}""".trimMargin()
            ).contentType("application/json")
        } else {
            val result = if (e.details.isEmpty()) e.msg else """
                |${e.msg}
                |${e.details.map {
                """
                |${it.key}:
                |${it.value}
                |"""
            }.joinToString("")}""".trimMargin()
            ctx.status(e.status).result(result)
        }
    }

    private const val docsUrl = "https://javalin.io/documentation#"
    private fun classUrl(e: HttpResponseException) = docsUrl + e.javaClass.simpleName

    // this could be removed by introducing a "DefaultResponse", but I would
    // rather keep this ugly snippet than introduced another abstraction layer
    private fun getTypeUrl(e: HttpResponseException) = when (e) {
        is RedirectResponse -> classUrl(e)
        is BadRequestResponse -> classUrl(e)
        is UnauthorizedResponse -> classUrl(e)
        is ForbiddenResponse -> classUrl(e)
        is NotFoundResponse -> classUrl(e)
        is MethodNotAllowedResponse -> classUrl(e)
        is InternalServerErrorResponse -> classUrl(e)
        is ServiceUnavailableResponse -> classUrl(e)
        else -> docsUrl + "error-responses"
    }
}
