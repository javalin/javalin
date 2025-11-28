package io.javalin.testtools

import io.javalin.Javalin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestCookieIssueKotlin {

    @Test
    fun `can set and read session attributes`() {
        JavalinTest.test(Javalin.create { config ->
            config.routes.get("/set") { ctx -> ctx.sessionAttribute("foo", "bar") }
            config.routes.get("/get") { ctx -> ctx.result(ctx.sessionAttribute<String>("foo") ?: "") }
        }) { server, client ->
            client.get("/set") // Set session attribute
            assertThat(client.get("/get").body.string()).isEqualTo("bar")
        }
    }

    @Test
    fun `cookie handling works automatically`() {
        JavalinTest.test(Javalin.create { config ->
            config.routes.get("/set-cookie") { ctx -> ctx.cookie("test-cookie", "cookie-value") }
            config.routes.get("/get-cookie") { ctx -> 
                ctx.result(ctx.cookie("test-cookie") ?: "no-cookie")
            }
        }) { server, client ->
            // Verify cookie is set in response
            val setCookieHeaders = client.get("/set-cookie").headers().get("Set-Cookie")
            assertThat(setCookieHeaders).isNotNull()
            assertThat(setCookieHeaders!![0]).contains("test-cookie=cookie-value")

            // Verify cookie is automatically sent in subsequent request
            assertThat(client.get("/get-cookie").body.string()).isEqualTo("cookie-value")
        }
    }
}
