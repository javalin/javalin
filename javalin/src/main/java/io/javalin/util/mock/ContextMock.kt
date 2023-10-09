package io.javalin.util.mock

import io.javalin.Javalin
import io.javalin.compression.CompressionStrategy
import io.javalin.config.JavalinConfig
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.http.servlet.JavalinServletContextConfig
import io.javalin.router.HttpHandlerEntry
import io.javalin.util.mock.HttpServletRequestMock.RequestState
import io.javalin.util.mock.HttpServletResponseMock.ResponseState
import java.io.InputStream
import java.util.function.Consumer
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
@Suppress("DataClassPrivateConstructor")
data class ContextMock private constructor(
    private val javalinConfig: JavalinConfig?,
    private val requestStateCfg: Consumer<RequestState>,
    private val responseStateCfg: Consumer<ResponseState>
) {

    companion object {
        @JvmStatic
        fun create(): ContextMockBuilder = ContextMockBuilder()
    }

    class ContextMockBuilder(
        private var javalinConfig: JavalinConfig? = null,
        private var requestState: Consumer<RequestState> = Consumer {},
        private var responseState: Consumer<ResponseState> = Consumer {},
    ) {
        @JvmOverloads
        fun withJavalinConfiguration(javalin: Javalin = Javalin.create()): ContextMockBuilder = apply { this.javalinConfig = javalin.unsafeConfig() }
        fun withRequestState(requestState: Consumer<RequestState>): ContextMockBuilder = apply { this.requestState = requestState }
        fun withResponseState(responseState: Consumer<ResponseState>): ContextMockBuilder = apply { this.responseState = responseState }
        fun build(): ContextMock = ContextMock(javalinConfig, requestState, responseState)
    }

    fun create(requestState: RequestState, responseState: ResponseState): JavalinServletContext {
        val cfg = JavalinServletContextConfig(
            appAttributes = javalinConfig?.pvt?.appAttributes ?: emptyMap(),
            compressionStrategy = javalinConfig?.pvt?.compressionStrategy ?: CompressionStrategy.NONE,
            defaultContentType = javalinConfig?.http?.defaultContentType ?: ContentType.HTML,
            requestLoggerEnabled = false,
        )

        this.requestStateCfg.accept(requestState)
        val req = HttpServletRequestMock(requestState)

        this.responseStateCfg.accept(responseState)
        val res = HttpServletResponseMock(responseState)

        return JavalinServletContext(
            cfg = cfg,
            req = req,
            res = res,
        )
    }

    fun execute(body: Consumer<Context>): Context {
        val ctx = create(RequestState(), ResponseState())
        body.accept(ctx)
        return ctx
    }

    fun interface Body {
        fun toInputStream(): InputStream
    }
    class StringBody(val body: String) : Body {
        override fun toInputStream(): InputStream = body.byteInputStream()
    }

    @JvmOverloads
    fun execute(endpoint: Endpoint, uri: String = endpoint.path, body: Body? = null): Context {
        val requestState = RequestState().also {
            it.headers[Header.HOST] = mutableListOf("localhost")
            it.method = endpoint.method.name
            it.contextPath = javalinConfig?.router?.contextPath?.takeIf { it != "/" } ?: ""
            it.requestURI = uri
            it.requestURL = "${it.scheme}://${it.serverName}:${it.serverPort}${it.contextPath}${it.requestURI}"
            it.inputStream = body?.toInputStream() ?: it.inputStream
        }

        val ctx = create(
            requestState = requestState,
            responseState = ResponseState()
        )

        ctx.update(
            httpHandlerEntry = HttpHandlerEntry(
                type = endpoint.method,
                path = endpoint.path,
                routerConfig = javalinConfig?.router ?: Javalin.create().unsafeConfig().router,
                roles = emptySet(),
                handler = endpoint.handler
            ),
            requestUri = uri
        )

        endpoint.handler.handle(ctx)
        return ctx
    }

}
