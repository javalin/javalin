package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.http.Header
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.OK
import io.javalin.http.bodyAsClass
import io.javalin.testtools.TestTool.Companion.TestLogsKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration

class KotlinTest {

    class MyKotlinClass(
        val field1: String,
        val field2: String
    )

    @Test
    fun `get method works`() = JavalinTest.test(Javalin.create { config ->
        config.routes.get("/hello") { it.result("Hello, World!") }
    }) { server, client ->
        val response = client.get("/hello")
        assertThat(response.code).isEqualTo(OK.code)
        assertThat(response.body!!.string()).isEqualTo("Hello, World!")
    }

    @Test
    fun `can do query-params and headers`() = JavalinTest.test(Javalin.create { config ->
        config.routes.get("/hello") {
            val response = "${it.queryParam("from")} ${it.header(Header.FROM)}"
            it.result(response)
        }
    }) { server, client ->
        val response = client.get("/hello?from=From") { it.header(Header.FROM, "Paris to Berlin") }
        assertThat(response.body?.string()).isEqualTo("From Paris to Berlin")
    }

    @Test
    fun `post with json serialization works`() = JavalinTest.test(Javalin.create { config ->
        config.routes.post("/hello") { it.result(it.bodyAsClass<MyKotlinClass>().field1) }
    }) { server, client ->
        val response = client.post("/hello", MyKotlinClass("v1", "v2"))
        assertThat(response.body?.string()).isEqualTo("v1")
    }

    @Test
    fun `all common verbs work`() = JavalinTest.test(Javalin.create { config ->
        config.routes.get("/") { it.result("GET") }
        config.routes.post("/") { it.result("POST") }
        config.routes.patch("/") { it.result("PATCH") }
        config.routes.put("/") { it.result("PUT") }
        config.routes.delete("/") { it.result("DELETE") }
    }) { server, client ->
        assertThat(client.get("/").body?.string()).isEqualTo("GET")
        assertThat(client.post("/").body?.string()).isEqualTo("POST")
        assertThat(client.patch("/").body?.string()).isEqualTo("PATCH")
        assertThat(client.put("/").body?.string()).isEqualTo("PUT")
        assertThat(client.delete("/").body?.string()).isEqualTo("DELETE")
    }

    @Test
    fun `request method works`() = JavalinTest.test(Javalin.create { config ->
        config.routes.post("/form") { it.result(it.formParam("username")!!) }
    }) { server, client ->
        val response = client.request("/form") {
            it.post(FormBody.Builder().add("username", "test").build())
        }
        assertThat(response.body!!.string()).isEqualTo("test")
    }

    @Test
    fun `custom javalin works`() {
        val app = Javalin.create { config ->
            config.routes.get("/hello") { it.result("Hello, World!") }
        }
        JavalinTest.test(app) { server, client ->
            assertThat(client.get("/hello").body?.string()).isEqualTo("Hello, World!")
        }
    }

    @Test
    fun `capture std out works`() = JavalinTest.test(Javalin.create { config ->
        val logger = LoggerFactory.getLogger(KotlinTest::class.java)
        config.routes.get("/hello") { ctx ->
            println("sout was called")
            logger.info("logger was called")
            throw Exception("an error occurred")
        }
    }) { server, client ->
        val stdOut = JavalinTest.captureStdOut { client.get("/hello") }
        assertThat(stdOut).contains("sout was called")
        assertThat(stdOut).contains("logger was called")
        assertThat(stdOut).contains("an error occurred")
    }

    @Test
    fun `testing full app works`() = JavalinTest.test(KotlinApp.app) { server, client ->
        assertThat(client.get("/hello").body?.string()).isEqualTo("Hello, app!");
        assertThat(client.get("/hello/").body?.string()).isEqualTo("Endpoint GET /hello/ not found"); // KotlinApp.app won't ignore trailing slashes
    }

    val javalinTest = TestTool(TestConfig(false))

    @Test
    fun `instantiate JavalinTest`() = javalinTest.test(Javalin.create { config ->
        config.routes.get("/hello") { ctx -> ctx.result("Hello world") }
    }) { server, client ->
        assertThat(client.get("/hello").body?.string()).isEqualTo("Hello world")
    }

    @Test
    fun `custom HttpClient is used`() {
        val app = Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello, ${ctx.header("X-Welcome")}!") }
        }

        val customHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

        val defaultHeaders = mapOf("X-Welcome" to "Javalin")

        JavalinTest.test(app, TestConfig(httpClient = customHttpClient, defaultHeaders = defaultHeaders)) { server, client ->
            assertThat(client.get("/hello").body?.string()).isEqualTo("Hello, Javalin!")
        }
    }

    @Test
    fun `exceptions in test code get re-thrown`() {
        assertThrows<Exception>("Error in test code") {
            JavalinTest.test { server, client ->
                throw Exception("Error in test code")
            }
        }
    }

    @Test
    fun `exceptions in handler code are caught by exception handler and not thrown`() {
        assertDoesNotThrow {
            JavalinTest.test(Javalin.create { config ->
                config.routes.get("/hello") {
                    throw Exception("Error in handler code")
                }
            }) { server, client ->
                assertThat(client.get("/hello").code).isEqualTo(INTERNAL_SERVER_ERROR.code)
            }
        }
    }

    @Test
    fun `exceptions in handler code is included in test logs`() {
        val app = Javalin.create { config ->
            config.routes.get("/hello") {
                throw Exception("Error in handler code")
            }
        }
        try {
            JavalinTest.test(app) { server, client ->
                assertThat(client.get("/hello").code).isEqualTo(OK.code)
            }
        } catch (t: Throwable) {
            // Ignore
        }
        assertThat(app.unsafe.pvt.appDataManager.get(TestLogsKey)).contains("Error in handler code")
    }

    private fun throwingTest(app: Javalin) {
        JavalinTest.test(app) { _, _ ->
            assertThat(false).isTrue()
        }
    }

    @Test
    fun `errors should contain valid stacktrace of origin exception`() {
        val app = Javalin.create()
        val exception = assertThrows<AssertionError> {
            throwingTest(app)
        }
        assertThat(exception.stackTrace.any { it.toString().contains("io.javalin.testtools.KotlinTest.throwingTest") }).isTrue
    }

    @Test
    fun `response headers are accessible`() = JavalinTest.test(Javalin.create { config ->
        config.routes.get("/headers") { ctx ->
            ctx.header("Custom-Header", "custom-value")
            ctx.header("Another-Header", "another-value")
            ctx.result("Response with headers")
        }
    }) { server, client ->
        val response = client.get("/headers")
        assertThat(response.headers().get("Custom-Header")).isNotNull().containsExactly("custom-value")
        assertThat(response.headers().get("Another-Header")).isNotNull().containsExactly("another-value")
        assertThat(response.headers().get("Non-Existent")).isNull()
    }

    @Test
    fun `empty and null response bodies work`() = JavalinTest.test(Javalin.create { config ->
        config.routes.get("/empty") { ctx -> ctx.result("") }
        config.routes.get("/null") { } // No result set
    }) { server, client ->
        assertThat(client.get("/empty").body?.string()).isEqualTo("")
        assertThat(client.get("/null").body?.string()).isEqualTo("")
    }

    @Test
    fun `request builder with multiple headers works`() = JavalinTest.test(Javalin.create { config ->
        config.routes.post("/multi-headers") { ctx ->
            ctx.result("Auth: ${ctx.header("Authorization")}, Accept: ${ctx.header("Accept")}, Custom: ${ctx.header("X-Custom")}")
        }
    }) { server, client ->
        val response = client.request("/multi-headers") { builder ->
            builder.post(HttpRequest.BodyPublishers.ofString("test-body"))
                   .header("Authorization", "Bearer token123")
                   .header("Accept", "application/json")
                   .header("X-Custom", "test-value")
        }

        assertThat(response.body?.string()).isEqualTo("Auth: Bearer token123, Accept: application/json, Custom: test-value")
    }

    @Test
    fun `different http methods with custom bodies work`() = JavalinTest.test(Javalin.create { config ->
        config.routes.put("/text") { ctx -> ctx.result("PUT: ${ctx.body()}") }
        config.routes.patch("/text") { ctx -> ctx.result("PATCH: ${ctx.body()}") }
        config.routes.delete("/text") { ctx -> ctx.result("DELETE: ${ctx.body()}") }
    }) { server, client ->
        assertThat(client.request("/text") { it.put(HttpRequest.BodyPublishers.ofString("plain text")).header("Content-Type", "text/plain") }.body?.string()).isEqualTo("PUT: plain text")
        assertThat(client.request("/text") { it.patch(HttpRequest.BodyPublishers.ofString("patch data")).header("Content-Type", "text/plain") }.body?.string()).isEqualTo("PATCH: patch data")
        assertThat(client.request("/text") { it.delete(HttpRequest.BodyPublishers.ofString("delete data")).header("Content-Type", "text/plain") }.body?.string()).isEqualTo("DELETE: delete data")
    }

}
