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
import io.javalin.router.EndpointExecutor
import io.javalin.util.mock.stubs.HttpServletRequestMock
import io.javalin.util.mock.stubs.HttpServletResponseMock
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer
import org.jetbrains.annotations.ApiStatus.Experimental

/**
 * A [ContextMock] is an in-memory [Context] instance builder.
 *
 * Although this implementation can be used in different ways, the most common use case is to build a [Context] instance within the test scope.
 * We're strongly recommending to use [ContextMock] over any other reflection based mocking library, as it's way closer to the real implementation.
 *
 * By default, the request state represents an incoming connection from localhost to the root path.
 * Javalin configuration, request and response states can be modified by using the [MockConfigurer] interface.
 * Once the state is prepared, you can build a [Context] instance in two ways:
 * - [ContextMock.build] with [Endpoint] instance: to simulate a real request/response cycle (recommended)
 * - [ContextMock.execute]: to execute non-endpoint related code that requires a [Context] instance
 *
 * See docs for more information: https://javalin.io/documentation#context-mock
 */
@Experimental
class ContextMock private constructor(
    private val mockConfig: MockConfig = MockConfig(),
    private val userConfigs: List<MockConfigurer> = emptyList(),
) : EndpointExecutor {

    companion object {

        @JvmStatic
        @JvmOverloads
        fun create(configurer: MockConfigurer? = null): ContextMock =
            ContextMock(
                userConfigs = configurer?.let { listOf(it) } ?: emptyList()
            )

    }

    /** Register additional [MockConfigurer]s. Each [ContextMock] can have multiple configurers, which are applied in registration order. */
    fun withMockConfig(cfg: MockConfigurer): ContextMock =
        ContextMock(
            mockConfig = mockConfig.clone(),
            userConfigs = userConfigs + cfg
        )

    /** Build an [EndpointExecutor] with a [uri], [Body] or/and a [configurer]. */
    @JvmOverloads
    fun build(uri: String? = null, body: Body? = null, configurer: MockConfigurer? = null): EndpointExecutor =
        EndpointExecutor { endpoint ->
            execute(endpoint, uri ?: endpoint.path, body, configurer)
        }

    /** Build an [EndpointExecutor] with a [Body]. */
    fun build(body: Body? = null, configurer: MockConfigurer? = null): EndpointExecutor =
        build(null, body, configurer)

    /** Execute a non-endpoint related code that requires [Context] instance **/
    fun execute(body: Consumer<Context>): Context {
        val (req, res) = createMockReqAndRes()
        val ctx = JavalinServletContext(createServletContextConfig(), req = req, res = res)
        body.accept(ctx)
        return ctx
    }

    /** Execute this ContextMock without any additional parameters **/
    override fun execute(endpoint: Endpoint): Context {
        return build().execute(endpoint)
    }

    private fun execute(endpoint: Endpoint, uri: String = endpoint.path, body: Body? = null, configurer: MockConfigurer? = null): Context {
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
        configurer?.let { invokeMockConfigurerWithAsSamWithReceiver(it, mockConfig) }

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
