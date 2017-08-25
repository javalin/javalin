/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.builder

class CookieBuilder(
        val name: String,
        val value: String,
        var domain: String = "",
        var path: String = "",
        var maxAge: Int = -1,
        var secure: Boolean = false,
        var httpOnly: Boolean = false) {

    companion object {
        @JvmStatic
        fun cookieBuilder(name: String, value: String): CookieBuilder {
            return CookieBuilder(name, value)
        }
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

}
