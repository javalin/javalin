/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

open class HttpResponseException @JvmOverloads constructor(
    val status: Int,
    message: String,
    val details: Map<String, String> = emptyMap()
) : RuntimeException(message) {

    @JvmOverloads constructor(
        status: HttpCode,
        message: String = status.message,
        details: Map<String, String> = emptyMap()
    ) : this(status.status, message, details)

}

open class RedirectResponse @JvmOverloads constructor(
    val location: String,
    status: Int,
    message: String,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(status, message, details) {

    @JvmOverloads constructor(
        location: String,
        status: HttpCode,
        message: String = status.message,
        details: Map<String, String> = emptyMap()
    ) : this(location, status.status, message, details)

}

class TemporaryRedirectResponse @JvmOverloads constructor(
    location: String,
    message: String = HttpCode.TEMPORARY_REDIRECT.message,
    details: Map<String, String> = emptyMap()
) : RedirectResponse(location, HttpCode.TEMPORARY_REDIRECT, message, details) {

    constructor(
        location: String,
        details: Map<String, String>,
    ) : this(location, HttpCode.TEMPORARY_REDIRECT.message, details)

}

class PermanentRedirectResponse @JvmOverloads constructor(
    location: String,
    message: String = HttpCode.PERMANENT_REDIRECT.message,
    details: Map<String, String> = emptyMap()
) : RedirectResponse(location, HttpCode.PERMANENT_REDIRECT, message, details) {

    constructor(
        location: String,
        details: Map<String, String>,
    ) : this(location, HttpCode.PERMANENT_REDIRECT.message, details)

}

class FoundRedirectResponse @JvmOverloads constructor(
    location: String,
    message: String = HttpCode.FOUND.message,
    details: Map<String, String> = emptyMap()
) : RedirectResponse(location, HttpCode.FOUND, message, details) {

    constructor(
        location: String,
        details: Map<String, String>,
    ) : this(location, HttpCode.FOUND.message, details)

}

class BadRequestResponse @JvmOverloads constructor(
    message: String = HttpCode.BAD_REQUEST.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.BAD_REQUEST, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.BAD_REQUEST.message, details)

}

class UnauthorizedResponse @JvmOverloads constructor(
    message: String = HttpCode.UNAUTHORIZED.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.UNAUTHORIZED, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.UNAUTHORIZED.message, details)

}

class ForbiddenResponse @JvmOverloads constructor(
    message: String = HttpCode.FORBIDDEN.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.FORBIDDEN, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.FORBIDDEN.message, details)

}

class NotFoundResponse @JvmOverloads constructor(
    message: String = HttpCode.NOT_FOUND.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.NOT_FOUND, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.NOT_FOUND.message, details)

}

class MethodNotAllowedResponse @JvmOverloads constructor(
    message: String = HttpCode.METHOD_NOT_ALLOWED.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.METHOD_NOT_ALLOWED, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.METHOD_NOT_ALLOWED.message, details)

}

class ConflictResponse @JvmOverloads constructor(
    message: String = HttpCode.CONFLICT.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.CONFLICT, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.CONFLICT.message, details)

}

class GoneResponse @JvmOverloads constructor(
    message: String = HttpCode.GONE.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.GONE, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.GONE.message, details)

}

class InternalServerErrorResponse @JvmOverloads constructor(
    message: String = HttpCode.INTERNAL_SERVER_ERROR.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.INTERNAL_SERVER_ERROR, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.INTERNAL_SERVER_ERROR.message, details)

}

class BadGatewayResponse @JvmOverloads constructor(
    message: String = HttpCode.BAD_GATEWAY.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.BAD_GATEWAY, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.BAD_GATEWAY.message, details)

}

class ServiceUnavailableResponse @JvmOverloads constructor(
    message: String = HttpCode.SERVICE_UNAVAILABLE.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.SERVICE_UNAVAILABLE, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.SERVICE_UNAVAILABLE.message, details)

}

class GatewayTimeoutResponse @JvmOverloads constructor(
    message: String = HttpCode.GATEWAY_TIMEOUT.message,
    details: Map<String, String> = emptyMap()
) : HttpResponseException(HttpCode.GATEWAY_TIMEOUT, message, details) {

    constructor(details: Map<String, String>) : this(HttpCode.GATEWAY_TIMEOUT.message, details)

}
