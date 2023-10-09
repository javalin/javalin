package io.javalin

import io.javalin.http.Context
import io.javalin.http.HandlerType.GET
import io.javalin.http.Header
import io.javalin.http.Header.HOST
import io.javalin.http.Header.X_FORWARDED_FOR
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.util.mock.ContextMock
import io.javalin.util.mock.Endpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestStubMocks {

    object TestController {
        fun getHelloWorld(ctx: Context) {
            ctx.result("Hello ${ctx.ip()}").status(IM_A_TEAPOT)
        }
    }

    private val endpoint = Endpoint(
        method = GET,
        path = "/api/{simple}/<complex>",
        handler = TestController::getHelloWorld
    )

    private val contextMock = ContextMock.create()

    @Test
    fun `should handle result`() {
        val context = contextMock.execute(endpoint)
        assertThat(context.result()).isEqualTo("Hello 127.0.0.1")
    }

    @Test
    fun `should handle status`() {
        val context = contextMock.execute(endpoint)
        assertThat(context.status()).isEqualTo(IM_A_TEAPOT)
    }

    @Test
    fun `should handle url related methods`() {
        val context = contextMock.execute(endpoint, "/api/simple/comp/lex")
        assertThat(context.scheme()).isEqualTo("http")
        assertThat(context.host()).isEqualTo("127.0.0.1")
        assertThat(context.method()).isEqualTo(GET)
        assertThat(context.url()).isEqualTo("http://localhost:80/api/simple/comp/lex")
        assertThat(context.path()).isEqualTo("/api/simple/comp/lex")
        assertThat(context.matchedPath()).isEqualTo("/api/{simple}/<complex>")
        assertThat(context.pathParam("simple")).isEqualTo("simple")
        assertThat(context.pathParam("complex")).isEqualTo("comp/lex")
    }

    @Test
    fun `should apply test level configuration`() {
        val context = contextMock
            .withJavalinConfiguration {
                it.contextResolver.ip = { ctx -> ctx.header(X_FORWARDED_FOR) ?: ctx.header(HOST)!! }
            }
            .withRequestState {
                it.headers[X_FORWARDED_FOR] = mutableListOf("1.9.9.9")
            }
            .execute(endpoint)

        assertThat(context.result()).isEqualTo("Hello 1.9.9.9")
    }

}
