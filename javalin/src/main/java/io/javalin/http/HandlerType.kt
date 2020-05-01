/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.util.Header
import javax.servlet.http.HttpServletRequest

enum class HandlerType {

    GET, POST, PUT, PATCH, DELETE, HEAD, TRACE, CONNECT, OPTIONS, BEFORE, AFTER, INVALID;

    fun isHttpMethod() = when (this) {
        GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH -> true
        else -> false
    }

    companion object {
        private val methodMap = values().map { it.toString() to it }.toMap()
        fun fromServletRequest(httpRequest: HttpServletRequest): HandlerType {
            val key = httpRequest.getHeader(Header.X_HTTP_METHOD_OVERRIDE) ?: httpRequest.method
            return methodMap[key.toUpperCase()] ?: INVALID
        }
    }

}
