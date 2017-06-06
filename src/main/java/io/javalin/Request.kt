/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.RequestUtil
import io.javalin.core.util.Util
import io.javalin.translator.json.Jackson
import org.slf4j.LoggerFactory
import java.io.IOException
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

class Request(private val servletRequest: HttpServletRequest,
              private val paramMap: Map<String, String>, // cache (different for each handler)
              private val splatList: List<String>) { // cache (different for each handler)

    private val log = LoggerFactory.getLogger(Request::class.java)

    private var passedToNextHandler: Boolean = false

    fun unwrap(): HttpServletRequest = servletRequest

    fun async(asyncHandler: AsyncHandler) {
        val asyncContext = servletRequest.startAsync()
        asyncHandler().thenAccept { _ -> asyncContext.complete() }
                .exceptionally { e ->
                    asyncContext.complete()
                    throw RuntimeException(e)
                }
    }

    fun body(): String = RequestUtil.byteArrayToString(bodyAsBytes(), servletRequest.characterEncoding)

    fun bodyAsBytes(): ByteArray {
        try {
            return RequestUtil.toByteArray(servletRequest.inputStream)
        } catch (e: IOException) {
            log.error("Failed to read body. Something is very wrong.", e)
            throw RuntimeException("Failed to read body. Something is very wrong.")
        }
    }

    fun <T> bodyAsClass(clazz: Class<T>): T {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return Jackson.toObject(body(), clazz)
    }

    fun bodyParam(bodyParam: String): String? = formParam(bodyParam)

    fun formParam(formParam: String): String? {
        return body().split("&")
                .map { it.split("=") }
                .filter { it.first().equals(formParam, ignoreCase = true) }
                .map { it.last() }
                .firstOrNull()
    }

    fun param(param: String): String? = paramMap[":" + param.toLowerCase().replaceFirst(":", "")]

    fun paramMap(): Map<String, String> = paramMap.toMap()

    fun splat(splatNr: Int): String? = splatList[splatNr]

    fun splats(): Array<String> = splatList.toTypedArray()

    // wrapper methods for HttpServletRequest

    fun attribute(attribute: String, value: Any) = servletRequest.setAttribute(attribute, value)

    fun <T> attribute(attribute: String): T = servletRequest.getAttribute(attribute) as T

    fun <T> attributeMap(): Map<String, T> = servletRequest.attributeNames.asSequence().map { it to servletRequest.getAttribute(it) as T }.toMap()

    fun contentLength(): Int = servletRequest.contentLength

    fun contentType(): String? = servletRequest.contentType

    fun cookie(name: String): String? = (servletRequest.cookies ?: arrayOf<Cookie>()).find { it.name == name }?.value

    fun cookieMap(): Map<String, String> = (servletRequest.cookies ?: arrayOf<Cookie>()).map { it.name to it.value }.toMap()

    fun header(header: String): String? = servletRequest.getHeader(header)

    fun headerMap(): Map<String, String> = servletRequest.headerNames.asSequence().map { it to servletRequest.getHeader(it) }.toMap()

    fun host(): String? = servletRequest.getHeader("host")

    fun ip(): String = servletRequest.remoteAddr

    fun next() {
        passedToNextHandler = true
    }

    fun nexted(): Boolean = passedToNextHandler

    fun path(): String? = servletRequest.pathInfo

    fun port(): Int = servletRequest.serverPort

    fun protocol(): String = servletRequest.protocol

    fun queryParam(queryParam: String): String? = servletRequest.getParameter(queryParam)

    fun queryParamOrDefault(queryParam: String, defaultValue: String): String = servletRequest.getParameter(queryParam) ?: defaultValue

    fun queryParams(queryParam: String): Array<String>? = servletRequest.getParameterValues(queryParam)

    fun queryParamMap(): Map<String, Array<String>> = servletRequest.parameterMap

    fun queryString(): String? = servletRequest.queryString

    fun requestMethod(): String = servletRequest.method

    fun scheme(): String = servletRequest.scheme

    fun uri(): String = servletRequest.requestURI

    fun url(): String = servletRequest.requestURL.toString()

    fun userAgent(): String? = servletRequest.getHeader("user-agent")

}
