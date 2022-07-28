package io.javalin.http

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Cookie as ServletCookie

const val SAME_SITE = "SameSite"

enum class SameSite(val value: String) {
    NONE("$SAME_SITE=None"),
    STRICT("$SAME_SITE=Strict"),
    LAX("$SAME_SITE=Lax");

    override fun toString() = this.value
}

data class Cookie @JvmOverloads constructor(
    var name: String,
    var value: String,
    var path: String = "/",
    var maxAge: Int = -1,
    var secure: Boolean = false,
    var version: Int = 0,
    var isHttpOnly: Boolean = false,
    var comment: String? = null,
    var domain: String? = null,
    var sameSite: SameSite? = null
)

fun HttpServletResponse.setJavalinCookie(javalinCookie: Cookie) {
    val cookie = ServletCookie(javalinCookie.name, javalinCookie.value).apply {
        this.path = javalinCookie.path
        this.maxAge = javalinCookie.maxAge
        this.secure = javalinCookie.secure
        this.version = javalinCookie.version
        this.isHttpOnly = javalinCookie.isHttpOnly
        this.comment = javalinCookie.comment
        if (javalinCookie.domain != null) {
            // Null check required as Servlet API does domain.toLowerCase()
            // with the amusing comment "IE allegedly needs this"
            this.domain = javalinCookie.domain
        }
    }
    this.addCookie(cookie) // we rely on this method for formatting the cookie header
    (this.getHeaders(Header.SET_COOKIE) ?: listOf()).toMutableList().let { cookies -> // mutable list of all cookies
        cookies.removeIf { it.startsWith("${cookie.name}=") && !it.contains(cookie.value) } // remove old cookie if duplicate name
        cookies.removeFirst()?.let { first -> this.setHeader(Header.SET_COOKIE, first.addSameSite(javalinCookie)) } // remove first cookie and use it to clear the header
        cookies.forEach { remaining -> this.addHeader(Header.SET_COOKIE, remaining.addSameSite(javalinCookie)) } // add all remaining cookies
    }

}

fun HttpServletResponse.removeCookie(name: String, path: String?) =
    this.addCookie(jakarta.servlet.http.Cookie(name, "").apply {
        this.path = path
        this.maxAge = 0
    })

fun HttpServletRequest.getCookie(name: String): String? =
    this.cookies?.find { it.name == name }?.value

fun String.addSameSite(cookie: Cookie): String {
    if (cookie.sameSite == null || this.contains(SAME_SITE)) return this
    return "$this; ${cookie.sameSite}"
}
