/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import java.util.*
import javax.servlet.http.HttpServletRequest

@FunctionalInterface
interface Handler {

    @Throws(Exception::class)
    fun handle(request: Request, response: Response)

    enum class Type {
        GET, POST, PUT, PATCH, DELETE, HEAD, TRACE, CONNECT, OPTIONS, BEFORE, AFTER, INVALID;

        companion object {
            private val methodMap = Handler.Type.values().map { it.toString() to it }.toMap()
            fun fromServletRequest(httpRequest: HttpServletRequest): Type {
                val key = Optional.ofNullable(httpRequest.getHeader("X-HTTP-Method-Override")).orElse(httpRequest.method)
                return methodMap.getOrDefault(key.toUpperCase(), INVALID)
            }
        }
    }
}
