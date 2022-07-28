package io.javalin.http

import io.javalin.http.util.ContextUtil
import io.javalin.http.util.CookieStore
import io.javalin.http.util.MultipartUtil
import io.javalin.plugin.json.JSON_MAPPER_KEY
import io.javalin.plugin.json.JsonMapper
import io.javalin.routing.HandlerEntry
import io.javalin.util.JavalinLogger
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicReference

class ContextState(
    internal var req: HttpServletRequest,
    internal var res: HttpServletResponse,
    var appAttributes: Map<String, Any> = mapOf(),
    var resultReference: AtomicReference<Result<out Any?>> = AtomicReference(Result()),
    var pathParamMap: Map<String, String> = mapOf(),
    var matchedPath: String = "",
    var endpointHandlerPath: String = "",
    var handlerType: HandlerType = HandlerType.BEFORE,
) {

    val cookieStore by lazy { CookieStore(req, res, appAttributes[JSON_MAPPER_KEY] as JsonMapper) }

    val characterEncoding by lazy { req.getHeader(Header.CONTENT_TYPE)
        ?.let { it.split(";").find { it.trim().startsWith("charset", ignoreCase = true) }?.let { it.split("=")[1].trim() } }
        ?: "UTF-8" }

    val method by lazy { HandlerType.findByName(req.getHeader(Header.X_HTTP_METHOD_OVERRIDE) ?: req.method) }

    val body by lazy {
        val maxRequestSize = this.appAttributes[ContextUtil.MAX_REQUEST_SIZE_KEY] as Long
        if (this.req.contentLength > maxRequestSize) {
            JavalinLogger.warn("Body greater than max size ($maxRequestSize bytes)")
            throw HttpResponseException(HttpCode.CONTENT_TOO_LARGE.status, HttpCode.CONTENT_TOO_LARGE.message)
        }
        req.inputStream.readBytes()
    }

    val formParams by lazy {
        if (MultipartUtil.isMultipartFormData(req.getHeader(Header.CONTENT_TYPE))) MultipartUtil.getFieldMap(req)
        else ContextUtil.splitKeyValueStringAndGroupByKey(body.toString(Charset.forName(characterEncoding)), characterEncoding)
    }

    val queryParams by lazy {
        ContextUtil.splitKeyValueStringAndGroupByKey(req.queryString ?: "", characterEncoding)
    }

    fun update(handlerEntry: HandlerEntry, requestUri: String) {
        this.matchedPath = handlerEntry.path
        this.pathParamMap = handlerEntry.extractPathParams(requestUri)
        this.handlerType = handlerEntry.type
        if (this.handlerType != HandlerType.AFTER) {
            this.endpointHandlerPath = handlerEntry.path
        }
    }

    fun changeBaseRequest(req: HttpServletRequest) {
        this.req = req;
    }

}
