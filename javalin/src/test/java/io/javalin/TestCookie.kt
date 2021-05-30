package io.javalin

import com.mashape.unirest.http.Unirest
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

    /*
     * Requests
     */
    @Test
    fun `single cookie returns null when missing`() = TestUtil.test { app, http ->
        app.get("/read-cookie-1") { ctx -> ctx.result("" + ctx.cookie("my-cookie")) }
        assertThat(http.getBody("/read-cookie-1")).isEqualTo("null")
    }

    @Test
    fun `single cookie works`() = TestUtil.test { app, http ->
        app.get("/read-cookie-2") { ctx -> ctx.result(ctx.cookie("my-cookie")!!) }
        val response = Unirest.get("${http.origin}/read-cookie-2").header(Header.COOKIE, "my-cookie=my-cookie-value").asString()
        assertThat(response.body).isEqualTo("my-cookie-value")
    }

    @Test
    fun `cookie-map returns empty when no cookies are set`() = TestUtil.test { app, http ->
        app.get("/read-cookie-3") { ctx -> ctx.result(ctx.cookieMap().toString()) }
        assertThat(http.getBody("/read-cookie-3")).isEqualTo("{}")
    }

    @Test
    fun `cookie-map returns all cookies if cookies are set`() = TestUtil.test { app, http ->
        app.get("/read-cookie-4") { ctx -> ctx.result(ctx.cookieMap().toString()) }
        val response = Unirest.get("${http.origin}/read-cookie-4").header(Header.COOKIE, "k1=v1;k2=v2;k3=v3").asString()
        assertThat(response.body).isEqualTo("{k1=v1, k2=v2, k3=v3}")
    }

    /*
     * Responses
     */
    @Test
    fun `setting a cookie works`() = TestUtil.test { app, http ->
        app.get("/create-cookie") { ctx -> ctx.cookie("Test", "Tast") }
        app.get("/get-cookie") { ctx -> ctx.result(ctx.cookie("Test")!!) }
        assertThat(http.get("/create-cookie").headers.getFirst(Header.SET_COOKIE)).isEqualTo("Test=Tast; Path=/")
        assertThat(http.getBody("/get-cookie")).isEqualTo("Tast")
    }

    @Test
    fun `setting a Cookie object works`() = TestUtil.test { app, http ->
        app.get("/create-cookie") { ctx -> ctx.cookie(Cookie("Hest", "Hast").apply { maxAge = 7 }) }
        assertThat(http.get("/create-cookie").headers.getFirst(Header.SET_COOKIE)).contains("Hest=Hast")
        assertThat(http.get("/create-cookie").headers.getFirst(Header.SET_COOKIE)).contains("Max-Age=7")
    }

    @Test
    fun `can't set duplicate cookies when other cookies set`() = TestUtil.test { app, http ->
        app.get("/create-cookies") {
            it.cookie("Test-1", "1")
            it.cookie("Test-2", "2")
            it.cookie("Test-3", "3")
            it.cookie("Test-3", "4")  // duplicate
        }
        val response = http.get("/create-cookies");
        assertThat(response.headers[Header.SET_COOKIE]!!).contains("Test-1=1; Path=/")
        assertThat(response.headers[Header.SET_COOKIE]!!).contains("Test-2=2; Path=/")
        assertThat(response.headers[Header.SET_COOKIE]!!).contains("Test-3=4; Path=/")
        assertThat(response.headers[Header.SET_COOKIE]!!.size).isEqualTo(3)
    }

    @Test
    fun `can't set duplicate cookies when no other cookies set`() = TestUtil.test { app, http ->
        app.get("/create-cookies") {
            it.cookie("MyCookie", "A")
            it.cookie("MyCookie", "B")  // duplicate
        }
        val response = http.get("/create-cookies");
        assertThat(response.headers[Header.SET_COOKIE]!!).contains("MyCookie=B; Path=/")
        assertThat(response.headers[Header.SET_COOKIE]!!.size).isEqualTo(1)
    }

    private fun cookieIsEffectivelyRemoved(cookie: String, path: String): Boolean {
        val pathMatches = cookie.split(";")[1].split("=")[1] == path
        val expiresEpoch = cookie.split(";")[2] == " Expires=Thu, 01-Jan-1970 00:00:00 GMT"
        val maxAgeZero = cookie.split(";")[3] == " Max-Age=0"
        return pathMatches && expiresEpoch && maxAgeZero
    }
}
