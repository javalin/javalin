package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.http.Header
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.bodyAsClass
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory

class KotlinTest {

    class MyKotlinClass(
        val field1: String,
        val field2: String
    )

    @Test
    fun `get method works`() = JavalinTest.test { server, client ->
        server.get("/hello") { it.result("Hello, World!") }
        val response = client.get("/hello")
        assertThat(response.code).isEqualTo(OK.code)
        assertThat(response.body!!.string()).isEqualTo("Hello, World!")
    }

    @Test
    fun `can do query-params and headers`() = JavalinTest.test { server, client ->
        server.get("/hello") {
            val response = "${it.queryParam("from")} ${it.header(Header.FROM)}"
            it.result(response)
        }
        val response = client.get("/hello?from=From") { it.header(Header.FROM, "Paris to Berlin") }
        assertThat(response.body?.string()).isEqualTo("From Paris to Berlin")
    }

    @Test
    fun `post with json serialization works`() = JavalinTest.test { server, client ->
        server.post("/hello") { it.result(it.bodyAsClass<MyKotlinClass>().field1) }
        val response = client.post("/hello", MyKotlinClass("v1", "v2"))
        assertThat(response.body?.string()).isEqualTo("v1")
    }

    @Test
    fun `all common verbs work`() = JavalinTest.test { server, client ->
        server.get("/") { it.result("GET") }
        assertThat(client.get("/").body?.string()).isEqualTo("GET")

        server.post("/") { it.result("POST") }
        assertThat(client.post("/").body?.string()).isEqualTo("POST")

        server.patch("/") { it.result("PATCH") }
        assertThat(client.patch("/").body?.string()).isEqualTo("PATCH")

        server.put("/") { it.result("PUT") }
        assertThat(client.put("/").body?.string()).isEqualTo("PUT")

        server.delete("/") { it.result("DELETE") }
        assertThat(client.delete("/").body?.string()).isEqualTo("DELETE")
    }

    @Test
    fun `request method works`() = JavalinTest.test { server, client ->
        server.post("/form") { it.result(it.formParam("username")!!) }
        val response = client.request("/form") {
            it.post(FormBody.Builder().add("username", "test").build())
        }
        assertThat(response.body!!.string()).isEqualTo("test")
    }

    @Test
    fun `custom javalin works`() {
        val app = Javalin.create()
            .get("/hello") { it.result("Hello, World!") }
        JavalinTest.test(app) { server, client ->
            assertThat(client.get("/hello").body?.string()).isEqualTo("Hello, World!")
        }
    }

    @Test
    fun `capture std out works`() = JavalinTest.test { server, client ->
        val logger = LoggerFactory.getLogger(KotlinTest::class.java)
        server.get("/hello") { ctx ->
            println("sout was called")
            logger.info("logger was called")
            throw Exception("an error occurred")
        }
        val stdOut = JavalinTest.captureStdOut { client.get("/hello") }
        assertThat(stdOut).contains("sout was called")
        assertThat(stdOut).contains("logger was called")
        assertThat(stdOut).contains("an error occurred")
    }

    @Test
    fun `testing full app works`() = JavalinTest.test(KotlinApp.app) { server, client ->
        assertThat(client.get("/hello").body?.string()).isEqualTo("Hello, app!");
        assertThat(client.get("/hello/").body?.string()).isEqualTo(NOT_FOUND.message); // KotlinApp.app won't ignore trailing slashes
    }

    val javalinTest = TestTool(TestConfig(false))

    @Test
    fun `instantiate JavalinTest`() = javalinTest.test { server, client ->
        server.get("/hello") { ctx -> ctx.result("Hello world") }
        assertThat(client.get("/hello").body?.string()).isEqualTo("Hello world")
    }

    @Test
    fun `custom OkHttpClient is used`() {
        val app = Javalin.create()
            .get("/hello") { ctx -> ctx.result("Hello, ${ctx.header("X-Welcome")}!") }

        val okHttpClientAddingHeader = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val userRequest = chain.request()
                chain.proceed(
                    userRequest.newBuilder()
                        .addHeader("X-Welcome", "Javalin")
                        .build()
                )
            })
            .build()

        JavalinTest.test(app, TestConfig(okHttpClient = okHttpClientAddingHeader)) { server, client ->
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
            JavalinTest.test { server, client ->
                server.get("/hello") {
                    throw Exception("Error in handler code")
                }
                assertThat(client.get("/hello").code).isEqualTo(INTERNAL_SERVER_ERROR.code)
            }
        }
    }

    @Test
    fun `exceptions in handler code is included in test logs`() {
        val app = Javalin.create()

        try {
            JavalinTest.test(app) { server, client ->
                server.get("/hello") {
                    throw Exception("Error in handler code")
                }

                assertThat(client.get("/hello").code).isEqualTo(OK.code)
            }
        } catch (t: Throwable) {
            // Ignore
        }

        assertThat(app.attribute("testlogs") as String).contains("Error in handler code")
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

}
