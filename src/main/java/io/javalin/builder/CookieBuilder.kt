/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.builder

import javax.servlet.http.Cookie

class CookieBuilder(
        private val name: String,
        private val value: String,
        private var domain: String = "",
        private var path: String = "",
        private var maxAge: Int = -1,
        private var secure: Boolean = false,
        private var httpOnly: Boolean = false) {

    companion object {
        @JvmStatic
        fun cookieBuilder(name: String, value: String): CookieBuilder = CookieBuilder(name, value)
    }

    fun domain(domain: String): CookieBuilder = this.apply { this.domain = domain }

    fun path(path: String): CookieBuilder = this.apply { this.path = path }

    fun maxAge(maxAge: Int): CookieBuilder = this.apply { this.maxAge = maxAge }

    fun secure(secure: Boolean): CookieBuilder = this.apply { this.secure = secure }

    fun httpOnly(httpOnly: Boolean): CookieBuilder = this.apply { this.httpOnly = httpOnly }

    fun build(): Cookie {
        val cookie = Cookie(name, value)
        cookie.domain = domain
        cookie.path = path
        cookie.maxAge = maxAge
        cookie.secure = secure
        cookie.isHttpOnly = httpOnly
        return cookie
    }
}
