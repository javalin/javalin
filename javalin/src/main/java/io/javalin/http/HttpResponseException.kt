/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.http.HttpStatus.BAD_GATEWAY
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.CONFLICT
import io.javalin.http.HttpStatus.FORBIDDEN
import io.javalin.http.HttpStatus.FOUND
import io.javalin.http.HttpStatus.GATEWAY_TIMEOUT
import io.javalin.http.HttpStatus.GONE
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.SERVICE_UNAVAILABLE
import io.javalin.http.HttpStatus.UNAUTHORIZED

open class HttpResponseException @JvmOverloads constructor(
    val code: HttpStatusCode,
    message: String = code.message,
    val details: Map<String, String> = mapOf()
) : RuntimeException(message)

class RedirectResponse @JvmOverloads constructor(
    code: HttpStatusCode = FOUND,
    message: String = code.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(code, message, details)


class BadRequestResponse @JvmOverloads constructor(
    message: String = BAD_REQUEST.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(BAD_REQUEST, message, details)

class UnauthorizedResponse @JvmOverloads constructor(
    message: String = UNAUTHORIZED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(UNAUTHORIZED, message, details)

class ForbiddenResponse @JvmOverloads constructor(
    message: String = FORBIDDEN.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(FORBIDDEN, message, details)

class NotFoundResponse @JvmOverloads constructor(
    message: String = NOT_FOUND.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NOT_FOUND, message, details)

class MethodNotAllowedResponse @JvmOverloads constructor(
    message: String = METHOD_NOT_ALLOWED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(METHOD_NOT_ALLOWED, message, details)

class ConflictResponse @JvmOverloads constructor(
    message: String = CONFLICT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(CONFLICT, message, details)

class GoneResponse @JvmOverloads constructor(
    message: String = GONE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(GONE, message, details)

class InternalServerErrorResponse @JvmOverloads constructor(
    message: String = INTERNAL_SERVER_ERROR.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(INTERNAL_SERVER_ERROR, message, details)

class BadGatewayResponse @JvmOverloads constructor(
    message: String = BAD_GATEWAY.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(BAD_GATEWAY, message, details)

class ServiceUnavailableResponse @JvmOverloads constructor(
    message: String = SERVICE_UNAVAILABLE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(SERVICE_UNAVAILABLE, message, details)

class GatewayTimeoutResponse @JvmOverloads constructor(
    message: String = GATEWAY_TIMEOUT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(GATEWAY_TIMEOUT, message, details)
