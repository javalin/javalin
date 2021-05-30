package io.javalin

import io.javalin.core.util.Header
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.servlet.http.Cookie

class TestCookie {

    @Test
    fun `cookie set on unspecified path is set to root URL`() = TestUtil.test { app, http ->
        app.get("/cookie-path") { it.cookie(Cookie("key", "value").apply { path = "/" }) }

        val requestSetCookie = http.get("/cookie-path")
        val cookiePath = requestSetCookie.headers.getFirst(Header.SET_COOKIE).split(";")[1].replaceFirst(" ", "")

        assertThat(cookiePath).isEqualTo("Path=/")
    }

    @Test
    fun `removing cookie without specifying path will remove cookie set to root URL`() = TestUtil.test { app, http ->
        val cookie = Cookie("key", "value").apply {
            path = "/"
        }
        app.get("/cookie-path") { it.cookie(cookie) }
        app.get("/cookie-path-remove") { it.removeCookie(cookie.name) }

        val requestSetCookie = http.get("/cookie-path")
        assertThat(requestSetCookie.headers.getFirst(Header.SET_COOKIE)).isEqualTo("key=value; Path=/")
        val requestRemoveCookie = http.get("/cookie-path-remove")

        assertThat(cookieIsEffectivelyRemoved(requestRemoveCookie.headers.getFirst(Header.SET_COOKIE))).isTrue
    }

    @Test
    fun `removing cookie without specifying path will not remove cookie set to non-root URL`() = TestUtil.test { app, http ->
        val cookie = Cookie("key", "value").apply {
            path = "/some-path"
        }
        app.get("/cookie-path") { it.cookie(cookie) }
        app.get("/cookie-path-remove") { it.removeCookie(cookie.name) }

        val requestSetCookie = http.get("/cookie-path")
        assertThat(requestSetCookie.headers.getFirst(Header.SET_COOKIE)).isEqualTo("key=value; Path=/some-path")
        val requestRemoveCookie = http.get("/cookie-path-remove")

        assertThat(cookieIsEffectivelyRemoved(requestRemoveCookie.headers.getFirst(Header.SET_COOKIE))).isTrue
    }

    private fun cookieIsEffectivelyRemoved(cookie: String): Boolean {
        val expiresEpoch = cookie.split(";")[2] == " Expires=Thu, 01-Jan-1970 00:00:00 GMT"
        val maxAgeZero = cookie.split(";")[3] == " Max-Age=0"
        return expiresEpoch && maxAgeZero
    }
}
