package io.javalin

import io.javalin.http.Handler
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestEndpointWrapper {

    @Test
    fun `endpointWrapper can wrap handlers`() = TestUtil.test(Javalin.create { config ->
        config.router.endpointWrapper = { endpoint ->
            Handler { ctx ->
                ctx.result("Wrapped: ")
                endpoint.handler.handle(ctx)
            }
        }
    }) { app, http ->
        app.unsafe.routes.get("/hello") { it.result(it.result() + "Hello") } // append to existing result
        assertThat(http.getBody("/hello")).isEqualTo("Wrapped: Hello")
    }

    @Test
    fun `endpointWrapper can conditionally skip handlers on query param`() = TestUtil.test(Javalin.create { config ->
        config.router.endpointWrapper = { endpoint ->
            Handler { ctx ->
                when (ctx.queryParam("skip") == "true") {
                    true -> ctx.result("Skipped")
                    false -> endpoint.handler.handle(ctx)
                }
            }
        }
    }) { app, http ->
        app.unsafe.routes.get("/hello") { it.result("Hello") }
        assertThat(http.getBody("/hello?skip=true")).isEqualTo("Skipped")
        assertThat(http.getBody("/hello?skip=false")).isEqualTo("Hello")
    }

    @Test
    fun `endpointWrapper does not add extra endpoint to stack`() = TestUtil.test(Javalin.create { config ->
        config.router.endpointWrapper = { endpoint ->
            Handler { endpoint.handler.handle(it) }
        }
        config.routes.get("/") { it.result("Stack size: " + it.endpoints().list().size) }
    }) { app, http ->
        assertThat(http.getBody("/")).isEqualTo("Stack size: 1")
    }
}
