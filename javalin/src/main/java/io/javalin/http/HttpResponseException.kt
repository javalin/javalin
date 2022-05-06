/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

open class HttpResponseException @JvmOverloads constructor(val status: Int, message: String, val details: Map<String, String> = mapOf()) : RuntimeException(message)

class RedirectResponse @JvmOverloads constructor(
    status: Int = HttpCode.FOUND.status,
    message: String = "Redirected",
    details: Map<String, String> = mapOf()
) : HttpResponseException(status, message, details)


class BadRequestResponse @JvmOverloads constructor(
    message: String = "Bad request",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.BAD_REQUEST.status, message, details)

class UnauthorizedResponse @JvmOverloads constructor(
    message: String = "Unauthorized",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.UNAUTHORIZED.status, message, details)

class ForbiddenResponse @JvmOverloads constructor(
    message: String = "Forbidden",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.FORBIDDEN.status, message, details)

class NotFoundResponse @JvmOverloads constructor(
    message: String = "Not found",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.NOT_FOUND.status, message, details)

class MethodNotAllowedResponse @JvmOverloads constructor(
    message: String = "Method not allowed",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.METHOD_NOT_ALLOWED.status, message, details)

class ConflictResponse @JvmOverloads constructor(
    message: String = "Conflict",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.CONFLICT.status, message, details)

class GoneResponse @JvmOverloads constructor(
    message: String = "Gone",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.GONE.status, message, details)

class InternalServerErrorResponse @JvmOverloads constructor(
    message: String = "Internal server error",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.INTERNAL_SERVER_ERROR.status, message, details)

class BadGatewayResponse @JvmOverloads constructor(
    message: String = "Bad gateway",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.BAD_GATEWAY.status, message, details)

class ServiceUnavailableResponse @JvmOverloads constructor(
    message: String = "Service unavailable",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.SERVICE_UNAVAILABLE.status, message, details)

class GatewayTimeoutResponse @JvmOverloads constructor(
    message: String = "Gateway timeout",
    details: Map<String, String> = mapOf()
) : HttpResponseException(HttpCode.GATEWAY_TIMEOUT.status, message, details)
