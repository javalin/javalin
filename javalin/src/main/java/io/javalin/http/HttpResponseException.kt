/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import org.eclipse.jetty.http.HttpStatus

open class HttpResponseException(val status: Int, message: String, val details: Map<String, String> = mapOf()) : RuntimeException(message)

class RedirectResponse(status: Int = HttpStatus.FOUND_302, msg: String = "Redirected") : HttpResponseException(status, msg)

class BadRequestResponse(message: String = "Bad request") : HttpResponseException(HttpStatus.BAD_REQUEST_400, message)
class UnauthorizedResponse(message: String = "Unauthorized") : HttpResponseException(HttpStatus.UNAUTHORIZED_401, message)
class ForbiddenResponse(message: String = "Forbidden") : HttpResponseException(HttpStatus.FORBIDDEN_403, message)
class NotFoundResponse(message: String = "Not found") : HttpResponseException(HttpStatus.NOT_FOUND_404, message)
class MethodNotAllowedResponse(message: String = "Method not allowed", details: Map<String, String>) : HttpResponseException(HttpStatus.METHOD_NOT_ALLOWED_405, message, details)
class ConflictResponse(message: String = "Conflict") : HttpResponseException(HttpStatus.CONFLICT_409, message)
class GoneResponse(message: String = "Gone") : HttpResponseException(HttpStatus.GONE_410, message)

class InternalServerErrorResponse(message: String = "Internal server error") : HttpResponseException(HttpStatus.INTERNAL_SERVER_ERROR_500, message)
class BadGatewayResponse(message: String = "Bad gateway") : HttpResponseException(HttpStatus.BAD_GATEWAY_502, message)
class ServiceUnavailableResponse(message: String = "Service unavailable") : HttpResponseException(HttpStatus.SERVICE_UNAVAILABLE_503, message)
class GatewayTimeoutResponse(message: String = "Gateway timeout") : HttpResponseException(HttpStatus.GATEWAY_TIMEOUT_504, message)
