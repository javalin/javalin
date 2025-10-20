package io.javalin.mock

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.POST
import io.javalin.http.Header
import io.javalin.http.Header.AUTHORIZATION
import io.javalin.http.Header.HOST
import io.javalin.http.Header.X_FORWARDED_FOR
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.bodyAsClass
import io.javalin.http.headerAsClass
import io.javalin.router.Endpoint
import io.javalin.security.BasicAuthCredentials
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.util.*

internal class TestMockContext {

    object TestController {
        val defaultApiEndpoint = Endpoint.create(GET, "/api/{simple}/<complex>").handler { it.result("Hello ${it.ip()}").status(IM_A_TEAPOT) }
        val asyncApiEndpoint = Endpoint.create(GET, "/api/async").handler { it.async { it.result("Welcome to the future") } }
        val consumeBodyEndpoint = Endpoint.create(POST, "/api/consume").handler { it.result(it.body()) }
        val sessionEndpoint = Endpoint.create(GET, "/api/session").handler { it.sessionAttribute("a", "b") }
    }

    data class PandaDto(val name: String)

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
        assertThat(context.protocol()).isEqualTo("HTTP/1.1")
        assertThat(context.host()).isEqualTo("localhost:80")
        assertThat(context.method()).isEqualTo(GET)
        assertThat(context.url()).isEqualTo("http://localhost:80/api/simple/comp/lex")
        assertThat(context.path()).isEqualTo("/api/simple/comp/lex")
        assertThat(context.httpEndpoint()?.path).isEqualTo("/api/{simple}/<complex>")
        assertThat(context.pathParam("simple")).isEqualTo("simple")
        assertThat(context.pathParam("complex")).isEqualTo("comp/lex")
    }

    @Test
    @Suppress("UastIncorrectHttpHeaderInspection")
    fun `should handle header related methods`() {
        val context = Endpoint(GET, "/") { it.header("X-Key", "value") }.handle(contextMock.build {
            req.addHeader("Test", "007")
            req.addHeader(AUTHORIZATION, "Basic ${Base64.getEncoder().encodeToString("user:pass".toByteArray())}")
        })
        assertThat(context.header("Test")).isEqualTo("007")
        assertThat(context.basicAuthCredentials()).isEqualTo(BasicAuthCredentials("user", "pass"))
        assertThat(context.headerAsClass<Int>("Test").get()).isEqualTo(7)
        assertThat(context.res().getHeader("X-Key")).isEqualTo("value")
    }

    @Test
    fun `should handle string body`() {
        val context = TestController.consumeBodyEndpoint.handle(contextMock.build(Body.ofString("Panda")))
        assertThat(context.body()).isEqualTo("Panda")
        assertThat(context.contentType()).isEqualTo(ContentType.PLAIN)
        assertThat(context.contentLength()).isEqualTo(5)
    }

    @Test
    fun `should handle input stream body`() {
        val context = TestController.consumeBodyEndpoint.handle(contextMock.build(Body.ofInputStream("Panda".byteInputStream())))
        assertThat(context.body()).isEqualTo("Panda")
        assertThat(context.contentType()).isEqualTo(ContentType.OCTET_STREAM)
        assertThat(context.contentLength()).isEqualTo(5)
    }

    @Test
    fun `should handle object body`() {
        val context = TestController.consumeBodyEndpoint.handle(contextMock.build(Body.ofObject(PandaDto("Kim"))))
        assertThat(context.body()).isEqualTo("""{"name":"Kim"}""")
        assertThat(context.bodyAsClass<PandaDto>()).isEqualTo(PandaDto("Kim"))
        assertThat(context.contentType()).isEqualTo(ContentType.JSON)
        assertThat(context.contentLength()).isEqualTo(14)
    }

    @Test
    fun `should handle output stream`() {
        val outputStream = ByteArrayOutputStream()
        Endpoint(GET, "/") { ctx ->
            ctx.res().writer.use { it.print("Panda") }
        }.handle(contextMock.build(Body.ofString("Panda")) {
            res.outputStream = outputStream
        })
        assertThat(outputStream.toByteArray().decodeToString()).isEqualTo("Panda")
    }

    @Test
    fun `should handle multipart files`() {
        val context = Endpoint(POST, "/") {}.handle(contextMock.build { req.addPart("file", "panda.txt", "Panda".toByteArray()) })
        val file = context.uploadedFile("file")!!
        assertThat(file.filename()).isEqualTo("panda.txt")
        assertThat(file.extension()).isEqualTo(".txt")
        assertThat(file.contentType()).isEqualTo(ContentType.OCTET_STREAM)
        assertThat(file.size()).isEqualTo("Panda".toByteArray().size.toLong())
        assertThat(file.contentAndClose { it.readAllBytes() }).isEqualTo("Panda".toByteArray())
    }

    @Test
    fun `should handle sessions`() {
        val context = TestController.sessionEndpoint.handle(contextMock)
        assertThat(context.sessionAttribute<String>("a")).isEqualTo("b")
        assertThat(context.sessionAttributeMap()).isEqualTo(mapOf("a" to "b"))
        assertThat(context.cachedSessionAttribute<String>("a")).isEqualTo("b")

        val session = context.req().session
        assertThat(session.isNew).isTrue()
        assertThat(session.id).startsWith("mock-session-")

        session.invalidate()
        assertThrows<IllegalStateException> { session.invalidate() }
        assertThrows<IllegalStateException> { session.isNew }
    }

    @Test
    fun `should be handled as a real async request`() {
        val context = TestController.asyncApiEndpoint.handle(contextMock)
        assertThat(context.result()).isEqualTo("Welcome to the future")
    }

    @Test
    fun `should support custom javalin configuration`() {
        val context = TestController.defaultApiEndpoint.handle(contextMock.build("/api/simple/comp/lex") {
            javalinConfig { it.contextResolver.ip = { ctx -> ctx.header(X_FORWARDED_FOR) ?: ctx.header(HOST)!! } }
            req.headers[X_FORWARDED_FOR] = mutableListOf("1.9.9.9")
        })
        assertThat(context.result()).isEqualTo("Hello 1.9.9.9")
    }

    @Test
    @Disabled // TODO: Fix whatever is broken with this test
    fun `should return same defaults as regular unirest request to jetty`() {
        val app = Javalin.create {
            it.jetty.defaultPort = 0
        }.start()

        try {
            val endpointUrl = "/test/{test}/<tests>"
            val requestedUrl = "/test/ab/c/d"
            val userAgent = "javalin-mock/1.0"

            val mock = ContextMock.create().withMockConfig {
                this.req.serverPort = app.port()
                this.req.localPort = app.port()
                this.req.addHeader(Header.USER_AGENT, userAgent)
            }
            val mockedCtx = Endpoint(POST, endpointUrl) { it.result("Passed") }
                .handle(mock.build(requestedUrl, Body.ofObject(PandaDto("Kim"))))

            app.unsafe.pvt.internalRouter.addHttpEndpoint(Endpoint(POST, endpointUrl) { ctx ->
                // Jetty

                assertThat(mockedCtx.req().remoteAddr).isEqualTo(ctx.req().remoteAddr)
                assertThat(mockedCtx.req().remoteHost).isEqualTo(ctx.req().remoteHost)
                assertThat(mockedCtx.req().localAddr).isEqualTo(ctx.req().localAddr)
                assertThat(mockedCtx.req().localName).isEqualTo(ctx.req().localName)
                assertThat(mockedCtx.req().localPort).isEqualTo(ctx.req().localPort)
                assertThat(mockedCtx.req().serverName).isEqualTo(ctx.req().serverName)
                assertThat(mockedCtx.req().serverPort).isEqualTo(ctx.req().serverPort)

                // Context

                assertThat(mockedCtx.httpEndpoint()?.method).isEqualTo(ctx.httpEndpoint()?.method)
                assertThat(mockedCtx.httpEndpoint()?.path).isEqualTo(ctx.httpEndpoint()?.path)

                assertThat(mockedCtx.contentLength()).isEqualTo(ctx.contentLength())
                assertThat(mockedCtx.contentType()).isEqualTo(ctx.contentType())
                assertThat(mockedCtx.method()).isEqualTo(ctx.method())
                assertThat(mockedCtx.path()).isEqualTo(ctx.path())
                assertThat(mockedCtx.port()).isEqualTo(ctx.port())
                assertThat(mockedCtx.protocol()).isEqualTo(ctx.protocol())
                assertThat(mockedCtx.contextPath()).isEqualTo(ctx.contextPath())
                assertThat(mockedCtx.userAgent()).isEqualTo(ctx.userAgent())
                assertThat(mockedCtx.characterEncoding()).isEqualTo(ctx.characterEncoding())
                assertThat(mockedCtx.url()).isEqualTo(ctx.url())
                assertThat(mockedCtx.fullUrl()).isEqualTo(ctx.fullUrl())
                assertThat(mockedCtx.scheme()).isEqualTo(ctx.scheme())
                assertThat(mockedCtx.host()).isEqualTo(ctx.host())
                assertThat(mockedCtx.ip()).isEqualTo(ctx.ip())
                assertThat(mockedCtx.body()).isEqualTo(ctx.body())
                assertThat(mockedCtx.bodyAsBytes()).isEqualTo(ctx.bodyAsBytes())
                assertThat(mockedCtx.bodyAsClass(PandaDto::class.java)).isEqualTo(ctx.bodyAsClass(PandaDto::class.java))

                assertThat(mockedCtx.queryParamMap()).isEqualTo(ctx.queryParamMap())
                assertThat(mockedCtx.queryParam("test")).isEqualTo(ctx.queryParam("test"))
                assertThat(mockedCtx.formParamMap()).isEqualTo(ctx.formParamMap())
                assertThat(mockedCtx.formParam("test")).isEqualTo(ctx.formParam("test"))
                assertThat(mockedCtx.pathParamMap()).isEqualTo(ctx.pathParamMap())
                assertThat(mockedCtx.pathParam("test")).isEqualTo(ctx.pathParam("test"))
                assertThat(mockedCtx.attributeMap()).isEqualTo(ctx.attributeMap())
                assertThat(mockedCtx.attribute<String>("test")).isEqualTo(ctx.attribute<String>("test"))
                assertThat(mockedCtx.sessionAttributeMap()).isEqualTo(ctx.sessionAttributeMap())
                assertThat(mockedCtx.sessionAttribute<String>("test")).isEqualTo(ctx.sessionAttribute<String>("test"))
                assertThat(mockedCtx.cookieMap()).isEqualTo(ctx.cookieMap())
                assertThat(mockedCtx.cookie("test")).isEqualTo(ctx.cookie("test"))
                assertThat(mockedCtx.headerMap()).isEqualTo(ctx.headerMap())
                assertThat(mockedCtx.header("test")).isEqualTo(ctx.header("test"))
                assertThat(mockedCtx.uploadedFileMap()).isEqualTo(ctx.uploadedFileMap())

                ctx.result("Passed")
            })

            val response = Unirest.post("http://localhost:${app.port()}$requestedUrl")
                .body(PandaDto("Kim"))
                .contentType(ContentType.JSON)
                .header(Header.USER_AGENT, userAgent)
                .asString()
                .body
            assertThat(response).isEqualTo("Passed")
        } finally {
            app.stop()
        }
    }


}
