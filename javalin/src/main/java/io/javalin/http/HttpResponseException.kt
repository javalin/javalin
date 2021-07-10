/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import org.eclipse.jetty.http.HttpStatus

open class HttpResponseException @JvmOverloads constructor(val status: Int, message: String, val details: Map<String, String> = mapOf()) : RuntimeException(message)

class RedirectResponse @JvmOverloads constructor(
    status: Int = HttpStatus.FOUND_302,
    message: String = "Redirected",
    details: Map<String, String> = mapOf()
) : HttpResponseException(status, message, details)


class BadRequestResponse @JvmOverloads constructor(
    message: String = "Bad request",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.BAD_REQUEST_400, message, details)

class UnauthorizedResponse @JvmOverloads constructor(
    message: String = "Unauthorized",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.UNAUTHORIZED_401, message, details)

class ForbiddenResponse @JvmOverloads constructor(
    message: String = "Forbidden",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.FORBIDDEN_403, message, details)

class NotFoundResponse @JvmOverloads constructor(
    message: String = "Not found",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.NOT_FOUND_404, message, details)

class MethodNotAllowedResponse @JvmOverloads constructor(
    message: String = "Method not allowed",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.METHOD_NOT_ALLOWED_405, message, details)

class ConflictResponse @JvmOverloads constructor(
    message: String = "Conflict",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.CONFLICT_409, message, details)

class GoneResponse @JvmOverloads constructor(
    message: String = "Gone",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.GONE_410, message, details)

class InternalServerErrorResponse @JvmOverloads constructor(
    message: String = "Internal server error",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.INTERNAL_SERVER_ERROR_500, message, details)

class BadGatewayResponse @JvmOverloads constructor(
    message: String = "Bad gateway",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.BAD_GATEWAY_502, message, details)

class ServiceUnavailableResponse @JvmOverloads constructor(
    message: String = "Service unavailable",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.SERVICE_UNAVAILABLE_503, message, details)

class GatewayTimeoutResponse @JvmOverloads constructor(
    message: String = "Gateway timeout",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpStatus.GATEWAY_TIMEOUT_504, message, details)
