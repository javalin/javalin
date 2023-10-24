package io.javalin

import io.javalin.http.ContentType
import io.javalin.http.HandlerType.GET
import io.javalin.http.Header.HOST
import io.javalin.http.Header.X_FORWARDED_FOR
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.router.Endpoint
import io.javalin.util.mock.ContextMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestMockContext {

    object TestController {
        val defaultApiEndpoint = Endpoint(GET, "/api/{simple}/<complex>") { it.result("Hello ${it.ip()}").status(IM_A_TEAPOT) }
        val asyncApiEndpoint = Endpoint(GET, "/api/async") { it.async { it.result("Welcome to the future") } }
    }

    private val contextMock = ContextMock.create {
        it.req.contentType = ContentType.JAVASCRIPT
    }

    @Test
    fun `should handle result`() {
        val context = TestController.defaultApiEndpoint.handle(contextMock)
        assertThat(context.result()).isEqualTo("Hello 127.0.0.1")
    }

    @Test
    fun `should handle status`() {
        val context = TestController.defaultApiEndpoint.handle(contextMock)
        assertThat(context.status()).isEqualTo(IM_A_TEAPOT)
    }

    @Test
    fun `should handle url related methods`() {
        val context = TestController.defaultApiEndpoint.handle(contextMock.build("/api/simple/comp/lex"))
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
        val context = TestController.defaultApiEndpoint.handle(contextMock.build("/api/simple/comp/lex") {
            it.javalinConfiguration { it.contextResolver.ip = { ctx -> ctx.header(X_FORWARDED_FOR) ?: ctx.header(HOST)!! } }
            it.req.headers[X_FORWARDED_FOR] = mutableListOf("1.9.9.9")
        })
        assertThat(context.result()).isEqualTo("Hello 1.9.9.9")
    }

    @Test
    fun `should be handled as a real request`() {
        val context = TestController.asyncApiEndpoint.handle(contextMock)
        assertThat(context.result()).isEqualTo("Welcome to the future")
    }

}
