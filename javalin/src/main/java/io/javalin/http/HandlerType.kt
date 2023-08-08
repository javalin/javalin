/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

enum class HandlerType(val isHttpMethod: Boolean = true) {

    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    TRACE,
    CONNECT,
    OPTIONS,
    BEFORE(isHttpMethod = false),
    BEFORE_MATCHED(isHttpMethod = false),
    AFTER_MATCHED(isHttpMethod = false),
    AFTER(isHttpMethod = false),
    INVALID(isHttpMethod = false);

    companion object {

        private val methodMap = values().associateBy { it.toString() }

        fun findByName(name: String): HandlerType = methodMap[name.uppercase()] ?: INVALID

    }

}
