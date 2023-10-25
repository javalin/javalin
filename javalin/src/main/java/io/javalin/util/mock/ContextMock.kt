package io.javalin.util.mock

import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.servlet.JavalinServlet
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.http.servlet.JavalinServletContextConfig
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.servlet.Task
import io.javalin.http.servlet.TaskInitializer
import io.javalin.router.Endpoint
import io.javalin.router.Endpoint.EndpointExecutor
import io.javalin.util.mock.HttpServletRequestMock.RequestState
import io.javalin.util.mock.HttpServletResponseMock.ResponseState
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
@Suppress("DataClassPrivateConstructor")
data class ContextMock private constructor(
    private val mockConfig: MockConfig = MockConfig(
        req = RequestState(),
        res = ResponseState(),
    ),
    private val userConfigs: List<MockConfigurer> = emptyList(),
) : EndpointExecutor {

    companion object {
        @JvmStatic @JvmOverloads fun create(cfg: MockConfigurer? = null): ContextMock = ContextMock(userConfigs = cfg?.let { listOf(it) } ?: emptyList())
    }

    fun withMockConfig(cfg: MockConfigurer): ContextMock =
        copy(
            mockConfig = mockConfig.clone(),
            userConfigs = userConfigs + cfg
        )

    fun build(body: Body? = null, cfg: MockConfigurer? = null): EndpointExecutor = build(null, body, cfg)

    @JvmOverloads
    fun build(uri: String? = null, body: Body? = null, cfg: MockConfigurer? = null): EndpointExecutor =
        EndpointExecutor { endpoint ->
            execute(endpoint, uri ?: endpoint.path, body, cfg)
        }

    override fun execute(endpoint: Endpoint): Context {
        return build().execute(endpoint)
    }

    fun execute(body: Consumer<Context>): Context {
        val (req, res) = createMockReqAndRes()
        val ctx = JavalinServletContext(createServletContextConfig(), req = req, res = res)
        body.accept(ctx)
        return ctx
    }

    private fun execute(endpoint: Endpoint, uri: String = endpoint.path, body: Body? = null, cfg: MockConfigurer? = null): Context {
        val (request, response) = createMockReqAndRes()
        body?.init(mockConfig)
        mockConfig.req.also { req ->
            req.headers[Header.HOST] = mutableListOf(req.remoteAddr)
            req.method = endpoint.method.name
            req.contextPath = mockConfig.javalinConfig.router.contextPath.takeIf { it != "/" } ?: ""
            req.requestURI = uri
            req.requestURL = "${req.scheme}://${req.serverName}:${req.serverPort}${req.contextPath}${req.requestURI}"
            req.inputStream = body?.toInputStream() ?: req.inputStream
            req.contentType = body?.getContentType() ?: req.contentType
            req.contentLength = body?.getContentLength() ?: req.contentLength
        }
        cfg?.let { invokeMockConfigurerWithAsSamWithReceiver(it, mockConfig) }

        val await = CountDownLatch(1)
        val javalinServlet = JavalinServlet(mockConfig.javalinConfig)
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

    private fun createMockReqAndRes(): Pair<HttpServletRequestMock, HttpServletResponseMock> {
        userConfigs.forEach { invokeMockConfigurerWithAsSamWithReceiver(it, mockConfig) }
        val response = HttpServletResponseMock(mockConfig.res)
        val request = HttpServletRequestMock(mockConfig.req, response)
        return request to response
    }

    private fun createServletContextConfig(): JavalinServletContextConfig =
        JavalinServletContextConfig(
            appAttributes = mockConfig.javalinConfig.pvt.appAttributes,
            compressionStrategy = mockConfig.javalinConfig.pvt.compressionStrategy,
            defaultContentType = mockConfig.javalinConfig.http.defaultContentType,
            requestLoggerEnabled = false,
        )

}
