/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.Header
import org.eclipse.jetty.http.HttpStatus

open class HttpResponseException(val status: Int, val msg: String) : RuntimeException()

object HttpResponseExceptionMapper {
    fun map(e: HttpResponseException, ctx: Context) {
        if (ctx.header(Header.ACCEPT)?.contains("application/json") == true) {
            ctx.status(e.status).result("""{"status": ${e.status}, "message": "${e.msg}", "timestamp": ${System.currentTimeMillis()}}""").contentType("application/json")
        } else {
            ctx.status(e.status).result(e.msg)
        }
    }
}

class RedirectResponse(status: Int = HttpStatus.FOUND_302, msg: String = "Redirected") : HttpResponseException(status, msg)

class BadRequestResponse(message: String = "Bad request") : HttpResponseException(HttpStatus.BAD_REQUEST_400, message)
class NotAuthorizedResponse(message: String = "Unauthorized") : HttpResponseException(HttpStatus.UNAUTHORIZED_401, message)
class ForbiddenResponse(message: String = "Forbidden") : HttpResponseException(HttpStatus.FORBIDDEN_403, message)
class NotFoundResponse(message: String = "Not found") : HttpResponseException(HttpStatus.NOT_FOUND_404, message)

class InternalServerErrorResponse(message: String = "Internal server error") : HttpResponseException(HttpStatus.INTERNAL_SERVER_ERROR_500, message)
class ServiceUnavailableResponse(message: String = "Service unavailable") : HttpResponseException(HttpStatus.SERVICE_UNAVAILABLE_503, message)
