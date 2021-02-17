/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import org.eclipse.jetty.http.HttpStatus

open class HttpResponseException(val status: Int, message: String, val details: Map<String, String> = mapOf()) : RuntimeException(message)

class RedirectResponse(
        status: Int = HttpStatus.FOUND_302,
        message: String = "Redirected",
        details: Map<String, String> = mapOf()
) : HttpResponseException(status, message, details)

class BadRequestResponse(
        message: String = "Bad request",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.BAD_REQUEST_400, message, details)

class UnauthorizedResponse(
        message: String = "Unauthorized",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.UNAUTHORIZED_401, message, details)

class ForbiddenResponse(
        message: String = "Forbidden",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.FORBIDDEN_403, message, details)

class NotFoundResponse(
        message: String = "Not found",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.NOT_FOUND_404, message, details)

class MethodNotAllowedResponse(
        message: String = "Method not allowed",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.METHOD_NOT_ALLOWED_405, message, details)

class ConflictResponse(
        message: String = "Conflict",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.CONFLICT_409, message, details)

class GoneResponse(
        message: String = "Gone",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.GONE_410, message, details)

class InternalServerErrorResponse(
        message: String = "Internal server error",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.INTERNAL_SERVER_ERROR_500, message, details)

class BadGatewayResponse(
        message: String = "Bad gateway",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.BAD_GATEWAY_502, message, details)

class ServiceUnavailableResponse(
        message: String = "Service unavailable",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.SERVICE_UNAVAILABLE_503, message, details)

class GatewayTimeoutResponse(
        message: String = "Gateway timeout",
        details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.GATEWAY_TIMEOUT_504, message, details)
