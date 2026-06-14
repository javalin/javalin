package io.javalin.http

import io.javalin.http.HandlerType.GET
import io.javalin.security.RouteRole
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil

import kong.unirest.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

class TestCustomHttpMethods {

    // JDK client, not Unirest: HttpMethod.valueOf(...) leaks tokens into Unirest's global method list.
    private fun rawRequestStatus(http: HttpUtil, method: String, path: String): Int =
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(http.origin + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build(),
            BodyHandlers.ofString()
        ).statusCode()

    val PROPFIND = HandlerType.findOrCreate("PROPFIND")

    @Test
    fun `custom HTTP methods work with full request lifecycle`() = TestUtil.test { app, http ->
        app.unsafe.routes.addHttpHandler(PROPFIND, "/webdav") { ctx -> ctx.result("PROPFIND response") }
        val response = http.call(HttpMethod.valueOf("PROPFIND"), "/webdav")
        assertThat(response.body).isEqualTo("PROPFIND response")
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun `HandlerType findOrCreate creates custom methods correctly`() {
        val customMethod = HandlerType.findOrCreate("PROPFIND")
        assertThat(customMethod.name()).isEqualTo("PROPFIND")
        assertThat(customMethod.isHttpMethod).isTrue()
        assertThat(customMethod.toString()).isEqualTo("PROPFIND")
    }

    @Test
    fun `HandlerType findByName returns existing constants for standard methods`() {
        val getMethod = HandlerType.findOrCreate("GET")
        assertThat(getMethod).isSameAs(GET)  // Should return the same instances as the static constants
    }

    @Test
    fun `HandlerType findByName throws for invalid method names`() {
        val invalidNames = listOf("", "GET1337", "INVALID METHOD", "123", "GET/POST")
        invalidNames.forEach { name ->
            assertThatThrownBy { HandlerType.findOrCreate(name) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid HTTP method name")
        }
    }

    @Test
    fun `HandlerType equality works correctly`() {
        val custom1 = HandlerType.findOrCreate("CUSTOM")
        val custom2 = HandlerType.findOrCreate("CUSTOM")
        val custom3 = HandlerType.findOrCreate("OTHER")

        // Same custom method should return same instance
        assertThat(custom1).isSameAs(custom2)
        assertThat(custom1).isEqualTo(custom2)
        assertThat(custom1.hashCode()).isEqualTo(custom2.hashCode())

        // Different custom methods should not be equal
        assertThat(custom1).isNotEqualTo(custom3)
        assertThat(custom1.hashCode()).isNotEqualTo(custom3.hashCode())
    }

    @Test
    fun `standard HTTP methods still work`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/test") { ctx -> ctx.result("GET works") }
        app.unsafe.routes.post("/test") { ctx -> ctx.result("POST works") }

        assertThat(http.get("/test").body).isEqualTo("GET works")
        assertThat(http.post("/test").body("").asString().body).isEqualTo("POST works")
    }

    @Test
    fun `custom methods work with middleware`() = TestUtil.test { app, http ->
        app.unsafe.routes.before("/webdav/*") { ctx -> ctx.header("X-WebDAV", "true") }
        app.unsafe.routes.addHttpHandler(PROPFIND, "/webdav/resource") { ctx ->
            val headerValue = ctx.res().getHeader("X-WebDAV") ?: "null"
            ctx.result("PROPFIND with middleware: $headerValue")
        }

        val response = http.call(HttpMethod.valueOf("PROPFIND"), "/webdav/resource")
        assertThat(response.body).isEqualTo("PROPFIND with middleware: true")
        assertThat(response.headers.getFirst("X-WebDAV")).isEqualTo("true")
    }

    @Test
    fun `custom methods work with path parameters`() = TestUtil.test { app, http ->
        app.unsafe.routes.addHttpHandler(PROPFIND, "/webdav/{resource}") { ctx ->
            ctx.result("PROPFIND resource: ${ctx.pathParam("resource")}")
        }

        val response = http.call(HttpMethod.valueOf("PROPFIND"), "/webdav/documents")
        assertThat(response.body).isEqualTo("PROPFIND resource: documents")
    }

    enum class TestRole : RouteRole { ADMIN, USER }

    @Test
    fun `custom methods work with roles`() = TestUtil.test { app, http ->
        val MKCOL = HandlerType.findOrCreate("MKCOL")
        app.unsafe.routes.addHttpHandler(MKCOL, "/webdav/collections", { ctx ->
            ctx.result("Collection created with roles: ${ctx.routeRoles()}")
        }, TestRole.ADMIN, TestRole.USER)

        val response = http.call(HttpMethod.valueOf("MKCOL"), "/webdav/collections")
        assertThat(response.body).isEqualTo("Collection created with roles: [ADMIN, USER]")
    }

    @Test
    fun `unrecognized request methods return 404 instead of 500`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("GET works") }
        // hyphenated and lowercase tokens must miss the router quietly, not throw (issue #2607)
        listOf("M-SEARCH", "VERSION-CONTROL", "get", "propfind", "FOOBAR").forEach { method ->
            assertThat(rawRequestStatus(http, method, "/")).`as`("method '$method'").isEqualTo(404)
        }
    }

    @Test
    fun `findOrDefault is lenient and does not pollute METHOD_MAP`() {
        assertThat(HandlerType.findOrDefault("GET")).isSameAs(GET)
        val unknown = HandlerType.findOrDefault("m-search")
        assertThat(unknown.name()).isEqualTo("m-search")
        // not cached, unlike findOrCreate, so untrusted tokens can't grow METHOD_MAP
        assertThat(HandlerType.findOrDefault("m-search")).isNotSameAs(unknown)
    }

    @Test
    fun `HandlerType commonHttp returns only HTTP methods`() {
        val commonHttpMethods = HandlerType.commonHttp()
        assertThat(commonHttpMethods).containsExactlyInAnyOrder(
            HandlerType.GET, HandlerType.POST, HandlerType.QUERY, HandlerType.PUT, HandlerType.DELETE,
            HandlerType.PATCH, HandlerType.HEAD, HandlerType.OPTIONS, HandlerType.TRACE
        )
    }
}
