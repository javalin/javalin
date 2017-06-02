/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.builder

import io.javalin.core.util.Util

//TODO: This auto-conversion could probably be cleaned up a lot
class CookieBuilder private constructor(private val name: String, private val value: String) {

    private var domain = ""
    private var path = ""
    private var maxAge = -1
    private var secure = false
    private var httpOnly = false

    companion object {
        fun cookieBuilder(name: String, value: String): CookieBuilder {
            return CookieBuilder(name, value)
        }
    }

    init {
        Util.notNull(name, "Cookie name cannot be null")
        Util.notNull(value, "Cookie value cannot be null")
    }

    fun domain(domain: String): CookieBuilder {
        this.domain = domain
        return this
    }

    fun path(path: String): CookieBuilder {
        this.path = path
        return this
    }

    fun maxAge(maxAge: Int): CookieBuilder {
        this.maxAge = maxAge
        return this
    }

    fun secure(secure: Boolean): CookieBuilder {
        this.secure = secure
        return this
    }

    fun httpOnly(httpOnly: Boolean): CookieBuilder {
        this.httpOnly = httpOnly
        return this
    }

    // getters
    fun name(): String {
        return name
    }

    fun value(): String {
        return value
    }

    fun domain(): String {
        return domain
    }

    fun path(): String {
        return path
    }

    fun maxAge(): Int {
        return maxAge
    }

    fun secure(): Boolean {
        return secure
    }

    fun httpOnly(): Boolean {
        return httpOnly
    }

}