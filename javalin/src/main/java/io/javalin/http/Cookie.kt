package io.javalin.http

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Cookie as ServletCookie

const val SAME_SITE = "SameSite"

/**
 * Value to define the `SameSite` property of a cookie.
 */
enum class SameSite(val value: String) {
    /** Means that the browser sends the cookie with both cross-site and same-site requests.*/
    NONE("$SAME_SITE=None"),

    /**
     * Means that the browser sends the cookie only for same-site requests,
     * that is, requests originating from the same site that set the cookie.
     * If a request originates from a different domain or scheme (even with
     * the same domain), no cookies with the SameSite=Strict attribute are sent.
     */
    STRICT("$SAME_SITE=Strict"),

    /**
     * Means that the cookie is not sent on cross-site requests, such as on
     * requests to load images or frames, but is sent when a user is navigating
     * to the origin site from an external site (for example, when following a link).
     * This is the default behavior if the SameSite attribute is not specified.
     */
    LAX("$SAME_SITE=Lax");

    override fun toString() = this.value
}

/**
 * An HTTP cookie (web cookie, browser cookie) is a small piece of data that a server
 * sends to a user's web browser. The browser may store the cookie and send it back
 * to the same server with later requests. Typically, an HTTP cookie is used to tell
 * if two requests come from the same browser.
 *
 * @param name the name of the cookie
 * @param value the value of the cookie
 * @param path indicates the path that must exist in the requested URL for the browser to
 * send the Cookie header. (default = "/")
 * @param maxAge indicates the number of seconds until the cookie expires.
 * A zero or negative number will expire the cookie immediately. (default = -1)
 * @param secure indicates that the cookie is sent to the server only when a request is
 * made with the https: scheme (except on localhost), and therefore, is more resistant
 * to man-in-the-middle attacks (default: false)
 * @param version the version of the protocol this cookie complies with (default: 0)
 * @param isHttpOnly if true, forbids JavaScript from accessing the cookie, for example,
 * through the `Document.cookie` property. (default: false)
 * @param comment a comment that describes a cookie's purpose (default: null)
 * @param domain defines the host to which the cookie will be sent. If null this attribute
 * defaults to the host of the current document URL, not including subdomains. (default: null)
 * @param sameSite controls whether or not a cookie is sent with cross-site requests,
 * providing some protection against cross-site request forgery attacks (CSRF). (default: null)
 */
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
        cookies.removeFirstOrNull()?.let { first -> this.setHeader(Header.SET_COOKIE, first.addSameSite(javalinCookie)) } // remove first cookie and use it to clear the header
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
