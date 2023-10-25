package io.javalin

import io.javalin.http.ContentType
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.POST
import io.javalin.http.Header.AUTHORIZATION
import io.javalin.http.Header.HOST
import io.javalin.http.Header.X_FORWARDED_FOR
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.bodyAsClass
import io.javalin.http.headerAsClass
import io.javalin.router.Endpoint
import io.javalin.security.BasicAuthCredentials
import io.javalin.util.mock.Body
import io.javalin.util.mock.ContextMock
import java.util.Base64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestMockContext {

    object TestController {
        val defaultApiEndpoint = Endpoint(GET, "/api/{simple}/<complex>") { it.result("Hello ${it.ip()}").status(IM_A_TEAPOT) }
        val asyncApiEndpoint = Endpoint(GET, "/api/async") { it.async { it.result("Welcome to the future") } }
        val consumeBody = Endpoint(POST, "/api/consume") { it.result(it.body()) }
    }

    private val contextMock = ContextMock.create {
        req.contentType = ContentType.JAVASCRIPT
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
    fun `should handle header related methods`() {
        val context = TestController.defaultApiEndpoint.handle(contextMock.build {
            req.addHeader("Test", "007")
            req.addHeader(AUTHORIZATION, "Basic ${Base64.getEncoder().encodeToString("user:pass".toByteArray())}")
        })
        assertThat(context.header("Test")).isEqualTo("007")
        assertThat(context.basicAuthCredentials()).isEqualTo(BasicAuthCredentials("user", "pass"))
        assertThat(context.headerAsClass<Int>("Test").get()).isEqualTo(7)
    }

    @Test
    fun `should handle string body`() {
        val context = TestController.consumeBody.handle(contextMock.build(Body.ofString("Panda")))
        assertThat(context.body()).isEqualTo("Panda")
        assertThat(context.contentType()).isEqualTo(ContentType.PLAIN)
        assertThat(context.contentLength()).isEqualTo(5)
    }

    @Test
    fun `should handle input stream body`() {
        val context = TestController.consumeBody.handle(contextMock.build(Body.ofInputStream("Panda".byteInputStream())))
        assertThat(context.body()).isEqualTo("Panda")
        assertThat(context.contentType()).isEqualTo(ContentType.OCTET_STREAM)
        assertThat(context.contentLength()).isEqualTo(5)
    }

    @Test
    fun `should handle object body`() {
        data class PandaDto(val name: String)
        val context = TestController.consumeBody.handle(contextMock.build(Body.ofObject(PandaDto("Kim"))))
        assertThat(context.body()).isEqualTo("""{"name":"Kim"}""")
        assertThat(context.bodyAsClass<PandaDto>()).isEqualTo(PandaDto("Kim"))
        assertThat(context.contentType()).isEqualTo(ContentType.JSON)
        assertThat(context.contentLength()).isEqualTo(14)
    }

    @Test
    fun `should be handled as a real async request`() {
        val context = TestController.asyncApiEndpoint.handle(contextMock)
        assertThat(context.result()).isEqualTo("Welcome to the future")
    }

    @Test
    fun `should support custom javalin configuration`() {
        val context = TestController.defaultApiEndpoint.handle(contextMock.build("/api/simple/comp/lex") {
            javalinConfiguration { it.contextResolver.ip = { ctx -> ctx.header(X_FORWARDED_FOR) ?: ctx.header(HOST)!! } }
            req.headers[X_FORWARDED_FOR] = mutableListOf("1.9.9.9")
        })
        assertThat(context.result()).isEqualTo("Hello 1.9.9.9")
    }

}
