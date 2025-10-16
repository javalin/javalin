package io.javalin

import io.javalin.testing.TestUtil

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestRoutesConfig {

    @Test
    fun `routes config provides direct access to HTTP verbs`() {
        val app = Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello, World!") }
            config.routes.post("/hello") { ctx -> ctx.result("Posted!") }
        }

        TestUtil.test(app) { _, http ->
            assertThat(http.get("/hello").body).isEqualTo("Hello, World!")
            val postResponse = http.post("/hello").asString()
            println("POST response status: ${postResponse.status}")
            println("POST response body: '${postResponse.body}'")
            assertThat(postResponse.body).isEqualTo("Posted!")
        }
    }

    @Test
    fun `routes config works alongside router config`() {
        val app = Javalin.create { config ->
            // Router settings
            config.router.contextPath = "/api"
            config.router.ignoreTrailingSlashes = true

            // Routes using new API
            config.routes.get("/test") { ctx -> ctx.result("Test response") }
        }

        TestUtil.test(app) { _, http ->
            assertThat(http.get("/api/test").body).isEqualTo("Test response")
            assertThat(http.get("/api/test/").body).isEqualTo("Test response") // trailing slash ignored
        }
    }

    @Test
    fun `routes config supports before and after handlers`() {
        val app = Javalin.create { config ->
            config.routes.before("/hello") { ctx -> ctx.header("X-Before", "true") }
            config.routes.get("/hello") { ctx -> ctx.result("Hello!") }
            config.routes.after("/hello") { ctx -> ctx.header("X-After", "true") }
        }

        TestUtil.test(app) { _, http ->
            val response = http.get("/hello")
            assertThat(response.body).isEqualTo("Hello!")
            assertThat(response.headers.getFirst("X-Before")).isEqualTo("true")
            assertThat(response.headers.getFirst("X-After")).isEqualTo("true")
        }
    }

    @Test
    fun `routes config supports exception handling`() {
        val app = Javalin.create { config ->
            config.routes.get("/error") { throw RuntimeException("Test error") }
            config.routes.exception(RuntimeException::class.java) { e, ctx ->
                ctx.status(500).result("Handled: ${e.message}")
            }
        }

        TestUtil.test(app) { _, http ->
            val response = http.get("/error")
            assertThat(response.status).isEqualTo(500)
            assertThat(response.body).isEqualTo("Handled: Test error")
        }
    }

    @Test
    fun `routes API respects router settings like contextPath`() {
        val app = Javalin.create { config ->
            // Router settings
            config.router.contextPath = "/api"
            config.router.ignoreTrailingSlashes = true

            // New API - respects router settings
            config.routes.get("/hello") { ctx -> ctx.result("Hello API") }
        }

        TestUtil.test(app) { _, http ->
            assertThat(http.get("/api/hello").body).isEqualTo("Hello API")
            // Routes respect router settings like contextPath
        }
    }

    @Test
    fun `routes config supports websockets and sse`() {
        val app = Javalin.create { config ->
            config.routes.sse("/sse") { client ->
                client.sendEvent("test", "Hello SSE!")
                client.close()
            }
            config.routes.ws("/ws") { ws ->
                ws.onConnect { ctx -> ctx.send("Hello WebSocket!") }
            }
        }

        TestUtil.test(app) { _, http ->
            val sseResponse = http.sse("/sse").get()
            assertThat(sseResponse.body).contains("event: test")
            assertThat(sseResponse.body).contains("data: Hello SSE!")
        }
    }

}
