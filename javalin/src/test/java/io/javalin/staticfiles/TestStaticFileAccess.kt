package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.ContentType
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.staticfiles.Location
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File


class TestStaticFileAccess {

    enum class MyRole : RouteRole { ROLE_ONE, ROLE_TWO}

    private fun addAuthentication(config: JavalinConfig) {
        config.routes.beforeMatched { ctx ->
            val role: RouteRole? = ctx.queryParam("role")?.let { role -> MyRole.valueOf(role) }
            val routeRoles = ctx.routeRoles()
            if (routeRoles != emptySet<RouteRole>() && role !in routeRoles) {
                throw UnauthorizedResponse()
            }
        }
    }

    private val defaultStaticResourceApp: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add("/public", Location.CLASSPATH, roles = setOf(MyRole.ROLE_ONE))
            addAuthentication(it)
        }
    }

    private val passingStaticFileConfigResourceApp: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add { staticFileConfig ->
                staticFileConfig.hostedPath = "/"
                staticFileConfig.directory = "/public"
                staticFileConfig.location = Location.CLASSPATH
                staticFileConfig.roles = setOf(MyRole.ROLE_ONE)
            }
            addAuthentication(it)
        }
    }

    private val externalStaticResourceApp: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add("src/test/external/", Location.EXTERNAL, roles = setOf(MyRole.ROLE_ONE))
            addAuthentication(it)
        }
    }
    private val multiLocationStaticResourceApp: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add("src/test/external/", Location.EXTERNAL, roles = setOf(MyRole.ROLE_ONE))
            it.staticFiles.add("/public/immutable", Location.CLASSPATH, roles = setOf(MyRole.ROLE_TWO))
            addAuthentication(it)
        }
    }

    @Test
    fun `Authentication works for overlapping route and file name`() = TestUtil.test(defaultStaticResourceApp) { app, http ->
        app.unsafe.routes.get("/file") { it.result("Test Route") }
        assertThat(callWithRole(http.origin, "/file", role = "ROLE_ONE")).isEqualTo("Test Route")
    }

    @Test
    fun `Authentication works for passing static config`() = TestUtil.test(passingStaticFileConfigResourceApp) { _, http ->
        assertThat(callWithRole(http.origin, "/html.html", "ROLE_TWO")).isEqualTo(UNAUTHORIZED.message)
        val response = http.get("/html.html?role=ROLE_ONE")
        assertThat(response.httpCode()).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body).contains("HTML works")
    }

    @Test
    fun `Authentication works for classPath location`() = TestUtil.test(defaultStaticResourceApp) { _, http ->
        assertThat(callWithRole(http.origin, "/html.html", "ROLE_TWO")).isEqualTo(UNAUTHORIZED.message)
        val response = http.get("/html.html?role=ROLE_ONE")
        assertThat(response.httpCode()).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body).contains("HTML works")
    }

    @Test
    fun `Authentication works for external location`() = TestUtil.test(externalStaticResourceApp) { _, http ->
        assertThat(callWithRole(http.origin, "/html.html", "ROLE_TWO")).isEqualTo(UNAUTHORIZED.message)
        val response = http.get("/html.html?role=ROLE_ONE")
        assertThat(response.httpCode()).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.body).contains("HTML works")
    }

    @Test
    fun `Authentication works for multiple location`() = TestUtil.test(multiLocationStaticResourceApp) { _, http ->
        assertThat(callWithRole(http.origin, "/txt.txt", "ROLE_TWO")).isEqualTo(UNAUTHORIZED.message)
        assertThat(callWithRole(http.origin, "/txt.txt", "ROLE_ONE")).isEqualTo(
            File("src/test/external/txt.txt").takeIf { it.exists() }?.readText())
        assertThat(http.get("/txt.txt?role=ROLE_ONE").httpCode()).isEqualTo(HttpStatus.OK)

        val content = javaClass.classLoader.getResourceAsStream("public/immutable/library-1.0.0.min.js")
            ?.bufferedReader()
            ?.use { it.readText() }
        assertThat(callWithRole(http.origin, "/library-1.0.0.min.js", "ROLE_ONE")).isEqualTo(UNAUTHORIZED.message)
        assertThat(callWithRole(http.origin, "/library-1.0.0.min.js", "ROLE_TWO")).isEqualTo(content)
        assertThat(http.get("/library-1.0.0.min.js?role=ROLE_TWO").httpCode()).isEqualTo(HttpStatus.OK)
    }

    private fun callWithRole(origin: String, path: String, role: String) =
        Unirest.get(origin + path).queryString("role", role).asString().body
}
