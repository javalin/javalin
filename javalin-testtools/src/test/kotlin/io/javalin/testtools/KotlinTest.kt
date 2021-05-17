package io.javalin.testtools

import io.javalin.Javalin
import okhttp3.FormBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.slf4j.LoggerFactory

class KotlinTest {

    @Test
    fun `normal get method works`() = TestUtil.test { server, client ->
        server.get("/hello") { it.result("Hello, World!") }
        val response = client.get("/hello")
        assertThat(response.code).isEqualTo(200)
        assertThat(response.body!!.string()).isEqualTo("Hello, World!")
    }

    @Test
    fun `getBody method works`() = TestUtil.test { server, client ->
        server.get("/hello") { it.result("Hello, World!") }
        assertThat(client.getBody("/hello")).isEqualTo("Hello, World!")
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
            assertThat(client.getBody("/hello")).isEqualTo("Hello, World!")
        }
    }

    @Test
    fun `capture std out works`() = TestUtil.test { server, client ->
        val logger = LoggerFactory.getLogger(KotlinTest::class.java)
        server.get("/hello") { ctx ->
            println("sout was called")
            logger.info("logger was called")
        }
        val stdOut = TestUtil.captureStdOut { client.getBody("/hello") }
        assertThat(stdOut).contains("sout was called")
        assertThat(stdOut).contains("logger was called")
    }

}
