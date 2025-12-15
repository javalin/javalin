package io.javalin

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHandlerWrapper {

    @Test
    fun `handlerWrapper can wrap handlers`() = TestUtil.test(Javalin.create { config ->
        config.router.handlerWrapper { endpoint ->
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
    fun `handlerWrapper can conditionally skip handlers on query param`() = TestUtil.test(Javalin.create { config ->
        config.router.handlerWrapper { endpoint ->
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
    fun `handlerWrapper does not add extra endpoint to stack`() = TestUtil.test(Javalin.create { config ->
        config.router.handlerWrapper { endpoint ->
            Handler { endpoint.handler.handle(it) }
        }
        config.routes.get("/") { it.result("Stack size: " + it.endpoints().list().size) }
    }) { app, http ->
        assertThat(http.getBody("/")).isEqualTo("Stack size: 1")
    }

    @Test
    fun `handlerWrapper wraps all endpoints by default`() {
        var wrapCalls = 0
        TestUtil.test(Javalin.create { config ->
            config.router.handlerWrapper { endpoint -> Handler { wrapCalls++; endpoint.handler.handle(it) } }
            config.routes.before { it.appendResult("$wrapCalls") }
            config.routes.beforeMatched { it.appendResult("$wrapCalls") }
            config.routes.get("/") { it.appendResult("$wrapCalls") }
            config.routes.after { it.appendResult("$wrapCalls") }
        }) { _, http ->
            assertThat(http.getBody("/")).isEqualTo("1234")
            assertThat(wrapCalls).isEqualTo(4)
        }
    }

    @Test
    fun `handlerWrapper can easily wrap just http endpoints`() {
        var wrapCalls = 0
        TestUtil.test(Javalin.create { config ->
            config.router.handlerWrapper { endpoint ->
                when (endpoint.method.isHttpMethod) {
                    true -> Handler { wrapCalls++; endpoint.handler.handle(it) }
                    false -> endpoint.handler
                }
            }
            config.routes.before { it.appendResult("$wrapCalls") } // no wrapper, sum=0
            config.routes.beforeMatched { it.appendResult("$wrapCalls") } // no wrapper, sum=0
            config.routes.get("/") { it.appendResult("$wrapCalls") } // wrapper, sum=1
            config.routes.after { it.appendResult("$wrapCalls") } // no wrapper, sum=1
        }) { _, http ->
            assertThat(http.getBody("/")).isEqualTo("0011")
            assertThat(wrapCalls).isEqualTo(1) // GET is the only http endpoint
        }

    }

    @Test
    fun `handlerWrapper propagates exceptions from wrapped handler`() {
        TestUtil.test(Javalin.create { config ->
            config.router.handlerWrapper { endpoint -> Handler { endpoint.handler.handle(it) } }
            config.routes.get("/") { throw IllegalStateException("Test exception") }
        }) { _, http ->
            assertThat(http.get("/").status).isEqualTo(500)
        }
    }

    private fun Context.appendResult(s: String) = this.result((result() ?: "") + s)

}
