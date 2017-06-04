/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import java.util.*
import javax.servlet.http.HttpServletRequest

enum class HandlerType {
    GET, POST, PUT, PATCH, DELETE, HEAD, TRACE, CONNECT, OPTIONS, BEFORE, AFTER, INVALID;

    companion object {
        private val methodMap = HandlerType.values().map { it.toString() to it }.toMap()
        fun fromServletRequest(httpRequest: HttpServletRequest): HandlerType {
            val key = Optional.ofNullable(httpRequest.getHeader("X-HTTP-Method-Override")).orElse(httpRequest.method)
            return methodMap.getOrDefault(key.toUpperCase(), INVALID)
        }
    }

}
