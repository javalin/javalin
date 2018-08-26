/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.*
import java.util.concurrent.CompletionException

object HttpResponseExceptionMapper {

    fun attachMappers(app: Javalin) {
        app.exception(CompletionException::class.java) { e, ctx ->
            if (e.cause is HttpResponseException) {
                handleException(e.cause as HttpResponseException, ctx)
            }
        }
        app.exception(HttpResponseException::class.java) { e, ctx ->
            handleException(e, ctx)
        }
    }

    private fun handleException(e: HttpResponseException, ctx: Context) {
        if (ctx.header(Header.ACCEPT)?.contains("application/json") == true) {
            ctx.status(e.status).result("""{
                |    "title": "${e.msg}",
                |    "status": ${e.status},
                |    "type": "${getTypeUrl(e).toLowerCase()}",
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
        is ConflictResponse -> classUrl(e)
        is GoneResponse -> classUrl(e)
        is InternalServerErrorResponse -> classUrl(e)
        is ServiceUnavailableResponse -> classUrl(e)
        is BadGatewayResponse -> classUrl(e)
        is GatewayTimeoutResponse -> classUrl(e)
        else -> docsUrl + "error-responses"
    }

}
