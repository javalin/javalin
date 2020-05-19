/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.util.Header
import io.javalin.http.util.JsonEscapeUtil
import java.util.concurrent.CompletionException

object HttpResponseExceptionMapper {

    fun canHandle(t: Throwable) = HttpResponseException::class.java.isAssignableFrom(t::class.java) // is HttpResponseException or subclass

    fun handle(exception: Exception, ctx: Context) {
        val e = unwrap(exception)
        if (ctx.header(Header.ACCEPT)?.contains("application/json") == true || ctx.res.contentType == "application/json") {
            ctx.status(e.status).result("""{
                |    "title": "${e.message?.jsonEscape()}",
                |    "status": ${e.status},
                |    "type": "${getTypeUrl(e).toLowerCase()}",
                |    "details": ${e.details.map { """{"${it.key}": "${it.value.jsonEscape()}"}""" }}
                |}""".trimMargin()
            ).contentType("application/json")
        } else {
            val result = if (e.details.isEmpty()) "${e.message}" else """
                |${e.message}
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
    private fun unwrap(e: Exception) = (if (e is CompletionException) e.cause else e) as HttpResponseException

    // this could be removed by introducing a "DefaultResponse", but I would
    // rather keep this ugly snippet than introduced another abstraction layer
    private fun getTypeUrl(e: HttpResponseException) = when (e) {
        is RedirectResponse -> classUrl(e)
        is BadRequestResponse -> classUrl(e)
        is UnauthorizedResponse -> classUrl(e)
        is ForbiddenResponse -> classUrl(e)
        is NotFoundResponse -> classUrl(e)
        is MethodNotAllowedResponse -> classUrl(e)
        is ConflictResponse -> classUrl(e)
        is GoneResponse -> classUrl(e)
        is InternalServerErrorResponse -> classUrl(e)
        is ServiceUnavailableResponse -> classUrl(e)
        is BadGatewayResponse -> classUrl(e)
        is GatewayTimeoutResponse -> classUrl(e)
        else -> docsUrl + "error-responses"
    }

    private fun String.jsonEscape() = JsonEscapeUtil.escape(this)

}
