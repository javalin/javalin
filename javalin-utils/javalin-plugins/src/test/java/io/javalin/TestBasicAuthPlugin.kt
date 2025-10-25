package io.javalin

import io.javalin.http.staticfiles.Location
import io.javalin.plugin.BasicAuthPlugin
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Base64

class TestBasicAuthPlugin {

    @Test
    fun `basic auth filter plugin works`() {
        val basicAuthApp = Javalin.create { cfg ->
            cfg.registerPlugin(BasicAuthPlugin {
                it.username = "u"
                it.password = "p"
            })
            cfg.staticFiles.add("/public", Location.CLASSPATH)
            cfg.routes.get("/hellopath") { it.result("Hello") }
        }
        JavalinTest.test(basicAuthApp) { _, http ->
            assertThat(http.get("/hellopath").body.string()).isEqualTo("Unauthorized")
            assertThat(http.get("/html.html").body.string()).contains("Unauthorized")

            val credentials = Base64.getEncoder().encodeToString("u:p".toByteArray())
            http.get("/hellopath") { it.header("Authorization", "Basic $credentials") }.let {
                assertThat(it.body.string()).isEqualTo("Hello")
            }
            http.get("/html.html") { it.header("Authorization", "Basic $credentials") }.let {
                assertThat(it.body.string()).contains("HTML works")
            }
        }
    }
}

