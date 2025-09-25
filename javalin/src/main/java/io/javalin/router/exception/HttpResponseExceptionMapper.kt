/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router.exception

import io.javalin.http.BadGatewayResponse
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.ContentType
import io.javalin.http.ContentType.APPLICATION_JSON
import io.javalin.http.ContentType.TEXT_PLAIN
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.GatewayTimeoutResponse
import io.javalin.http.GoneResponse
import io.javalin.http.Header
import io.javalin.http.HttpResponseException
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.MethodNotAllowedResponse
import io.javalin.http.NotFoundResponse
import io.javalin.http.RedirectResponse
import io.javalin.http.ServiceUnavailableResponse
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.util.JsonEscapeUtil
import java.util.*

object HttpResponseExceptionMapper {

    fun handle(e: HttpResponseException, ctx: Context) = when {
        ctx.header(Header.ACCEPT)?.contains(ContentType.HTML) == true || ctx.res().contentType == ContentType.HTML -> ctx.htmlResponse(e)
        ctx.header(Header.ACCEPT)?.contains(ContentType.JSON) == true || ctx.res().contentType == ContentType.JSON -> ctx.jsonResponse(e)
        else -> ctx.plainResponse(e)
    }

    private fun Context.htmlResponse(e: HttpResponseException) = 
        status(e.status).result(plainResult(e)).contentType(TEXT_PLAIN).setAllowHeaderIfApplicable(e)

    private fun Context.jsonResponse(e: HttpResponseException) = 
        status(e.status).result(jsonResult(e)).contentType(APPLICATION_JSON).setAllowHeaderIfApplicable(e)

    private fun Context.plainResponse(e: HttpResponseException) = 
        status(e.status).result(plainResult(e)).contentType(TEXT_PLAIN).setAllowHeaderIfApplicable(e)

    private fun Context.setAllowHeaderIfApplicable(e: HttpResponseException) = also {
        if (e is MethodNotAllowedResponse) {
            val allowedMethods = e.details["availableMethods"] ?: e.details["Available methods"]
            if (allowedMethods != null) {
                header(Header.ALLOW, allowedMethods)
            }
        }
    }

    internal fun jsonResult(e: HttpResponseException) =
        """|{
           |    "title": "${e.message?.jsonEscape()}",
           |    "status": ${e.status},
           |    "type": "${getTypeUrl(e).lowercase(Locale.ROOT)}",
           |    "details": {${e.details.map { """"${it.key}":"${it.value.jsonEscape()}"""" }.joinToString(",")}}
           |}""".trimMargin()

    private fun plainResult(e: HttpResponseException) =
        if (e.details.isEmpty()) "${e.message}" else """
                |${e.message}
                |${e.details.map { "\n${it.key}:\n${it.value}" }.joinToString("")}
                |""".trimMargin()

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

    private fun String.jsonEscape() = JsonEscapeUtil.escape(this)

}
