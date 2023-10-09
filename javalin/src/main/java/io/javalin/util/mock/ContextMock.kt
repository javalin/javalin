package io.javalin.util.mock

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
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
    private val javalinConfig: JavalinConfig,
    private val requestStateCfg: List<Consumer<RequestState>> = emptyList(),
    private val responseStateCfg: List<Consumer<ResponseState>> = emptyList()
) {

    companion object {
        @JvmStatic @JvmOverloads fun create(javalin: Javalin = Javalin.create()): ContextMock = ContextMock(javalin.unsafeConfig())
    }

    @JvmOverloads
    fun withJavalinConfiguration(config: Consumer<JavalinConfig> = Consumer {}): ContextMock = copy(javalinConfig = Javalin.create(config).unsafeConfig())
    fun withRequestState(requestState: Consumer<RequestState>): ContextMock = copy(requestStateCfg = requestStateCfg + requestState)
    fun withResponseState(responseState: Consumer<ResponseState>): ContextMock = copy(responseStateCfg = responseStateCfg + responseState)

    fun create(requestState: RequestState, responseState: ResponseState): JavalinServletContext {
        val cfg = JavalinServletContextConfig(
            appAttributes = javalinConfig.pvt.appAttributes,
            compressionStrategy = javalinConfig.pvt.compressionStrategy,
            defaultContentType = javalinConfig.http.defaultContentType,
            requestLoggerEnabled = false,
        )

        this.requestStateCfg.forEach { it.accept(requestState) }
        val req = HttpServletRequestMock(requestState)

        this.responseStateCfg.forEach { it.accept(responseState) }
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
        val requestState = RequestState().also { req ->
            req.headers[Header.HOST] = mutableListOf(req.remoteAddr)
            req.method = endpoint.method.name
            req.contextPath = javalinConfig.router.contextPath.takeIf { it != "/" } ?: ""
            req.requestURI = uri
            req.requestURL = "${req.scheme}://${req.serverName}:${req.serverPort}${req.contextPath}${req.requestURI}"
            req.inputStream = body?.toInputStream() ?: req.inputStream
        }

        val ctx = create(
            requestState = requestState,
            responseState = ResponseState()
        )

        ctx.update(
            httpHandlerEntry = HttpHandlerEntry(
                type = endpoint.method,
                path = endpoint.path,
                routerConfig = javalinConfig.router,
                roles = emptySet(),
                handler = endpoint.handler
            ),
            requestUri = uri
        )

        endpoint.handler.handle(ctx)
        return ctx
    }

}
