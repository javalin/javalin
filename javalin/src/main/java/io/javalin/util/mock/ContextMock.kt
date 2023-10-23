package io.javalin.util.mock

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.servlet.JavalinServlet
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.http.servlet.JavalinServletContextConfig
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.servlet.Task
import io.javalin.http.servlet.TaskInitializer
import io.javalin.router.Endpoint
import io.javalin.util.mock.HttpServletRequestMock.RequestState
import io.javalin.util.mock.HttpServletResponseMock.ResponseState
import java.io.InputStream
import java.util.concurrent.CountDownLatch
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

    fun execute(body: Consumer<Context>): Context {
        val response = createResponse()
        val ctx = JavalinServletContext(createServletContextConfig(), req = createRequest(response = response), res = response)
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
        val response = createResponse()
        val request = createRequest(requestState, response)

        val await = CountDownLatch(1)
        val javalinServlet = JavalinServlet(javalinConfig)
        (javalinServlet.requestLifecycle as MutableList<TaskInitializer<JavalinServletContext>>).add(
            TaskInitializer { submitTask, _, _, _ ->
                submitTask(LAST, Task(skipIfExceptionOccurred = false) {
                    await.countDown()
                })
            }
        )
        javalinServlet.router.addHttpEndpoint(endpoint)
        val ctx = javalinServlet.handle(request, response)!!
        await.await()

        return ctx
    }

    private fun createResponse(responseState: ResponseState = ResponseState()): HttpServletResponseMock {
        this.responseStateCfg.forEach { it.accept(responseState) }
        return HttpServletResponseMock(responseState)
    }

    private fun createRequest(requestState: RequestState = RequestState(), response: HttpServletResponseMock): HttpServletRequestMock {
        this.requestStateCfg.forEach { it.accept(requestState) }
        return HttpServletRequestMock(requestState, response)
    }

    private fun createServletContextConfig(): JavalinServletContextConfig =
        JavalinServletContextConfig(
            appAttributes = javalinConfig.pvt.appAttributes,
            compressionStrategy = javalinConfig.pvt.compressionStrategy,
            defaultContentType = javalinConfig.http.defaultContentType,
            requestLoggerEnabled = false,
        )

}
