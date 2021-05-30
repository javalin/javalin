package io.javalin

import io.javalin.core.util.Header
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.servlet.http.Cookie

class TestCookie {

    @Test
    fun `cookie set on unspecified path is set to root URL`() = TestUtil.test { app, http ->
        app.get("/cookie") { it.cookie(Cookie("key", "value").apply { path = "/" }) }

        val setCookieResponse = http.get("/cookie")
        val cookiePath = setCookieResponse.headers.getFirst(Header.SET_COOKIE).split(";")[1].replaceFirst(" ", "")

        assertThat(cookiePath).isEqualTo("Path=/")
    }

    @Test
    fun `removing cookie without specifying path will remove cookie set to root URL`() = TestUtil.test { app, http ->
        val cookie = Cookie("key", "value").apply {
            path = "/"
        }
        app.get("/cookie") { it.cookie(cookie) }
        app.get("/cookie-remove") { it.removeCookie(cookie.name) }
        val setCookieResponse = http.get("/cookie")
        assertThat(setCookieResponse.headers.getFirst(Header.SET_COOKIE)).isEqualTo("key=value; Path=/")

        val removeCookieResponse = http.get("/cookie-remove")

        assertThat(cookieIsEffectivelyRemoved(removeCookieResponse.headers.getFirst(Header.SET_COOKIE), "/")).isTrue
    }

    @Test
    fun `removing cookie without specifying path will not remove cookie set to non-root URL`() = TestUtil.test { app, http ->
        val cookie = Cookie("key", "value").apply {
            path = "/some-path"
        }
        app.get("/cookie") { it.cookie(cookie) }
        app.get("/cookie-remove") { it.removeCookie(cookie.name) }
        val setCookieResponse = http.get("/cookie")
        assertThat(setCookieResponse.headers.getFirst(Header.SET_COOKIE)).isEqualTo("key=value; Path=/some-path")

        val removeCookieResponse = http.get("/cookie-remove")

        assertThat(cookieIsEffectivelyRemoved(removeCookieResponse.headers.getFirst(Header.SET_COOKIE), "/some-path")).isFalse
    }

    private fun cookieIsEffectivelyRemoved(cookie: String, path: String): Boolean {
        val pathMatches = cookie.split(";")[1].split("=")[1] == path
        val expiresEpoch = cookie.split(";")[2] == " Expires=Thu, 01-Jan-1970 00:00:00 GMT"
        val maxAgeZero = cookie.split(";")[3] == " Max-Age=0"
        return pathMatches && expiresEpoch && maxAgeZero
    }
}
