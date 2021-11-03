package io.javalin.awslambda

import java.util.concurrent.CountDownLatch
import javax.servlet.http.HttpServletRequest
import com.amazonaws.serverless.proxy.*
import com.amazonaws.serverless.proxy.internal.servlet.*
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest
import com.amazonaws.serverless.proxy.model.AwsProxyRequest
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext
import com.amazonaws.serverless.proxy.model.AwsProxyResponse
import com.amazonaws.services.lambda.runtime.Context as LambdaContext
import com.amazonaws.serverless.proxy.RequestReader.LAMBDA_CONTEXT_PROPERTY
import com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_EVENT_PROPERTY
import com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_CONTEXT_PROPERTY
import com.amazonaws.serverless.proxy.RequestReader.HTTP_API_EVENT_PROPERTY
import com.amazonaws.serverless.proxy.RequestReader.HTTP_API_CONTEXT_PROPERTY
import io.javalin.Javalin
import io.javalin.http.JavalinServlet
import io.javalin.http.Context as JavalinContext


fun JavalinContext.lambdaContext(): LambdaContext? {
    return this.attribute<LambdaContext>(LAMBDA_CONTEXT_PROPERTY)
}
fun JavalinContext.apiGatewayEvent(): AwsProxyRequest? {
    return this.attribute<AwsProxyRequest>(API_GATEWAY_EVENT_PROPERTY)
}
fun JavalinContext.apiGatewayContext(): AwsProxyRequestContext? {
    return this.attribute<AwsProxyRequestContext>(API_GATEWAY_CONTEXT_PROPERTY)
}
fun JavalinContext.apiGatewayV2Event(): HttpApiV2ProxyRequest? {
    return this.attribute<HttpApiV2ProxyRequest>(HTTP_API_EVENT_PROPERTY)
}
fun JavalinContext.apiGatewayV2Context(): HttpApiV2ProxyRequestContext? {
    return this.attribute<HttpApiV2ProxyRequestContext>(HTTP_API_CONTEXT_PROPERTY)
}

fun getJavalinRestAwsProxyHandler(app: Javalin): JavalinAwsLambdaHandler<AwsProxyRequest, AwsProxyResponse> {
    val newHandler = JavalinAwsLambdaHandler(AwsProxyRequest::class.java, AwsProxyResponse::class.java,
        AwsProxyHttpServletRequestReader(),
        AwsProxyHttpServletResponseWriter(),
        AwsProxySecurityContextWriter(),
        AwsProxyExceptionHandler(),
        app.javalinServlet()
    )
    newHandler.initialize()
    return newHandler
}

fun getJavalinHttpApiV2AwsProxyHandler(app: Javalin): JavalinAwsLambdaHandler<HttpApiV2ProxyRequest, AwsProxyResponse> {
    val newHandler = JavalinAwsLambdaHandler(HttpApiV2ProxyRequest::class.java, AwsProxyResponse::class.java,
        AwsHttpApiV2HttpServletRequestReader(),
        AwsProxyHttpServletResponseWriter(true),
        AwsHttpApiV2SecurityContextWriter(),
        AwsProxyExceptionHandler(),
        app.javalinServlet()
    )
    newHandler.initialize()
    return newHandler
}

class JavalinAwsLambdaHandler<RequestType, ResponseType>(
    requestTypeClass: Class<RequestType>?,
    responseTypeClass: Class<ResponseType>?,
    requestReader: RequestReader<RequestType, HttpServletRequest>?,
    responseWriter: ResponseWriter<AwsHttpServletResponse, ResponseType>?,
    securityContextWriter: SecurityContextWriter<RequestType>?,
    exceptionHandler: ExceptionHandler<ResponseType>?,
    val javalinServlet: JavalinServlet
) :
    AwsLambdaServletContainerHandler<RequestType, ResponseType, HttpServletRequest, AwsHttpServletResponse>(
        requestTypeClass,
        responseTypeClass,
        requestReader,
        responseWriter,
        securityContextWriter,
        exceptionHandler
    ) {

    init {
        getServletContext().addServlet("javalin", javalinServlet)
    }

    companion object {
        @JvmStatic fun getRestAwsProxyHandler(app: Javalin): JavalinAwsLambdaHandler<AwsProxyRequest, AwsProxyResponse> {
            return getJavalinRestAwsProxyHandler(app);
        }

        @JvmStatic fun getHttpV2AwsProxyHandler(app: Javalin): JavalinAwsLambdaHandler<HttpApiV2ProxyRequest, AwsProxyResponse> {
            return getJavalinHttpApiV2AwsProxyHandler(app);
        }
    }

    override fun getContainerResponse(request: HttpServletRequest?, latch: CountDownLatch?): AwsHttpServletResponse {
        return AwsHttpServletResponse(request, latch)
    }

    override fun handleRequest(
        httpServletRequest: HttpServletRequest?,
        httpServletResponse: AwsHttpServletResponse?,
        lambdaContext: LambdaContext?
    ) {

        if (AwsHttpServletRequest::class.java.isAssignableFrom(httpServletRequest!!::class.java)) {
            (httpServletRequest as AwsHttpServletRequest).servletContext = getServletContext()
        }
        doFilter(httpServletRequest, httpServletResponse, javalinServlet);
    }
}
