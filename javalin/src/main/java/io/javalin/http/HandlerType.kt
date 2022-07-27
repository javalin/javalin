/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

enum class HandlerType {

    GET, POST, PUT, PATCH, DELETE, HEAD, TRACE, CONNECT, OPTIONS, BEFORE, AFTER, INVALID;

    fun isHttpMethod() = when (this) {
        GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH -> true
        else -> false
    }

    companion object {

        private val methodMap = values().associateBy { it.toString() }

        fun findByName(name: String): HandlerType =
            methodMap[name.uppercase()] ?: INVALID

    }

}
