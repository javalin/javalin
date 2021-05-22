package io.javalin.testtools

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.http.context.body
import okhttp3.FormBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.slf4j.LoggerFactory

class KotlinTest {

    class MyKotlinClass(
            val field1: String,
            val field2: String
    )

    @Test
    fun `get method works`() = TestUtil.test { server, client ->
        server.get("/hello") { it.result("Hello, World!") }
        val response = client.get("/hello")
        assertThat(response.code).isEqualTo(200)
        assertThat(response.body!!.string()).isEqualTo("Hello, World!")
    }

    @Test
    fun `can do query-params and headers`() = TestUtil.test { server, client ->
        server.get("/hello") {
            val response = "${it.queryParam("from")} ${it.header(Header.FROM)}"
            it.result(response)
        }
        val response = client.get("/hello?from=From") { it.header(Header.FROM, "Russia With Love") }
        assertThat(response.body?.string()).isEqualTo("From Russia With Love")
    }

    @Test
    fun `post with json serialization works`() = TestUtil.test { server, client ->
        server.post("/hello") { it.result(it.body<MyKotlinClass>().field1) }
        val response = client.post("/hello", MyKotlinClass("v1", "v2"))
        assertThat(response.body?.string()).isEqualTo("v1")
    }

    @Test
    fun `all common verbs work`() = TestUtil.test { server, client ->
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
    fun `request method works`() = TestUtil.test { server, client ->
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
        TestUtil.test(app) { server, client ->
            assertThat(client.get("/hello").body?.string()).isEqualTo("Hello, World!")
        }
    }

    @Test
    fun `capture std out works`() = TestUtil.test { server, client ->
        val logger = LoggerFactory.getLogger(KotlinTest::class.java)
        server.get("/hello") { ctx ->
            println("sout was called")
            logger.info("logger was called")
        }
        val stdOut = TestUtil.captureStdOut { client.get("/hello") }
        assertThat(stdOut).contains("sout was called")
        assertThat(stdOut).contains("logger was called")
    }

    @Test
    fun `testing full app works`() = TestUtil.test(KotlinApp.app) { server, client ->
        assertThat(client.get("/hello").body?.string()).isEqualTo("Hello, app!");
        assertThat(client.get("/hello/").body?.string()).isEqualTo("Not found"); // KotlinApp.app won't ignore trailing slashes
    }

}
