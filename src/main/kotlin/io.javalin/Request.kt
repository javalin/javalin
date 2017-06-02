/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.core.util.RequestUtil
import io.javalin.core.util.Util
import io.javalin.translator.json.Jackson
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

class Request(private val servletRequest: HttpServletRequest,
              private val paramMap: Map<String, String>, // cache (different for each handler)
              private val splatList: List<String>) { // cache (different for each handler)

    private var passedToNextHandler: Boolean = false

    fun unwrap(): HttpServletRequest {
        return servletRequest
    }

    @FunctionalInterface
    interface AsyncHandler {
        fun handle(): CompletableFuture<Void>
    }

    fun async(asyncHandler: AsyncHandler) {
        val asyncContext = servletRequest.startAsync()
        asyncHandler.handle()
                .thenAccept { _ -> asyncContext.complete() }
                .exceptionally { e ->
                    asyncContext.complete()
                    throw RuntimeException(e)
                }
    }

    fun body(): String {
        return RequestUtil.byteArrayToString(bodyAsBytes()!!, servletRequest.characterEncoding)
    }

    fun bodyAsBytes(): ByteArray? {
        try {
            return RequestUtil.toByteArray(servletRequest.inputStream)
        } catch (e: IOException) {
            return null
        }
    }

    fun <T> bodyAsClass(clazz: Class<T>): T {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return Jackson.toObject(body(), clazz)
    }

    // yeah, this is probably not the best solution
    fun bodyParam(bodyParam: String): String? {
        body().split("&").forEach({ keyAndValue ->
            val pair = keyAndValue.split("=")
            if (pair[0].equals(bodyParam, ignoreCase = true)) {
                return pair[1]
            }
        })
        return null
    }

    fun formParam(formParam: String): String? {
        return bodyParam(formParam)
    }

    fun param(param: String?): String? {
        var param: String = param ?: return null
        if (!param.startsWith(":")) {
            param = ":" + param
        }
        return paramMap[param.toLowerCase()]
    }

    fun paramMap(): Map<String, String> {
        return paramMap.toMap();
    }

    fun splat(splatNr: Int): String? {
        return splatList[splatNr]
    }

    fun splats(): Array<String> {
        return splatList.toTypedArray()
    }

    // wrapper methods for HttpServletRequest

    fun attribute(attribute: String, value: Any) {
        servletRequest.setAttribute(attribute, value)
    }

    fun <T> attribute(attribute: String): T {
        return servletRequest.getAttribute(attribute) as T
    }

    fun attributeMap(): Map<String, Any> {
        return servletRequest.attributeNames.asSequence().map { it to servletRequest.getAttribute(it) }.toMap()
    }

    fun contentLength(): Int {
        return servletRequest.contentLength
    }

    fun contentType(): String {
        return servletRequest.contentType
    }

    fun cookie(name: String): String? {
        val cookies = servletRequest.cookies ?: arrayOf<Cookie>()
        return cookies.find { it.name == name }?.value
    }

    fun cookieMap(): Map<String, String> {
        val cookies = servletRequest.cookies ?: arrayOf<Cookie>()
        return cookies.map { it.name to it.value }.toMap()
    }

    fun header(header: String): String {
        return servletRequest.getHeader(header)
    }

    fun headerMap(): Map<String, String> {
        return servletRequest.headerNames.asSequence().map { it to servletRequest.getHeader(it) }.toMap()
    }

    fun host(): String {
        return servletRequest.getHeader("host")
    }

    fun ip(): String {
        return servletRequest.remoteAddr
    }

    operator fun next() {
        passedToNextHandler = true
    }

    fun nexted(): Boolean {
        return passedToNextHandler
    }

    fun path(): String {
        return servletRequest.pathInfo
    }

    fun port(): Int {
        return servletRequest.serverPort
    }

    fun protocol(): String {
        return servletRequest.protocol
    }

    fun queryParam(queryParam: String): String? {
        return servletRequest.getParameter(queryParam)
    }

    fun queryParamOrDefault(queryParam: String, defaultValue: String): String {
        return Optional.ofNullable(servletRequest.getParameter(queryParam)).orElse(defaultValue)
    }

    fun queryParams(queryParam: String): Array<String>? {
        return servletRequest.getParameterValues(queryParam)
    }

    fun queryParamMap(): Map<String, Array<String>>? {
        return servletRequest.parameterMap
    }

    fun queryString(): String {
        return servletRequest.queryString
    }

    fun requestMethod(): String {
        return servletRequest.method
    }

    fun scheme(): String {
        return servletRequest.scheme
    }

    fun uri(): String {
        return servletRequest.requestURI
    }

    fun url(): String {
        return servletRequest.requestURL.toString()
    }

    fun userAgent(): String {
        return servletRequest.getHeader("user-agent")
    }

}
