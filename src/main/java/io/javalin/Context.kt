/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.builder.CookieBuilder
import io.javalin.core.util.ContextUtil
import io.javalin.core.util.Util
import io.javalin.translator.json.Jackson
import io.javalin.translator.template.Freemarker
import io.javalin.translator.template.Mustache
import io.javalin.translator.template.Thymeleaf
import io.javalin.translator.template.Velocity
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class Context(private val servletResponse: HttpServletResponse,
              private val servletRequest: HttpServletRequest,
              internal var paramMap: Map<String, String>,
              internal var splatList: List<String>) {

    private val log = LoggerFactory.getLogger(Context::class.java)

    private var passedToNextHandler: Boolean = false

    private var resultString: String? = null
    private var resultStream: InputStream? = null

    fun next() {
        passedToNextHandler = true
    }

    fun nexted(): Boolean = passedToNextHandler

    //
    // Request-ish methods
    //

    fun request(): HttpServletRequest = servletRequest

    fun async(asyncHandler: () -> CompletableFuture<Void>) {
        val asyncContext = servletRequest.startAsync()
        asyncHandler().thenAccept { _ -> asyncContext.complete() }
                .exceptionally { e ->
                    asyncContext.complete()
                    throw RuntimeException(e)
                }
    }

    fun body(): String = ContextUtil.byteArrayToString(bodyAsBytes(), servletRequest.characterEncoding)

    fun bodyAsBytes(): ByteArray = ContextUtil.toByteArray(servletRequest.inputStream)

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

    fun mapQueryParams(vararg keys: String): List<String>? = try {
        keys.map { servletRequest.getParameter(it) }.requireNoNulls().toList()
    } catch (e: IllegalArgumentException) {
        null
    }

    fun anyQueryParamNull(vararg keys: String) : Boolean = keys.filter { servletRequest.getParameter(it) == null }.isNotEmpty()

    fun param(param: String): String? = paramMap[":" + param.toLowerCase().replaceFirst(":", "")]

    fun paramMap(): Map<String, String> = paramMap.toMap()

    fun splat(splatNr: Int): String? = splatList[splatNr]

    fun splats(): Array<String> = splatList.toTypedArray()

    // wrapper methods for HttpServletRequest

    fun attribute(attribute: String, value: Any) = servletRequest.setAttribute(attribute, value)

    fun <T> attribute(attribute: String): T = servletRequest.getAttribute(attribute) as T

    fun <T> attributeMap(): Map<String, T> = servletRequest.attributeNames.asSequence().map { it to servletRequest.getAttribute(it) as T }.toMap()

    fun contentLength(): Int = servletRequest.contentLength

    fun contentType(): String = servletRequest.contentType

    fun cookie(name: String): String? = (servletRequest.cookies ?: arrayOf<Cookie>()).find { it.name == name }?.value

    fun cookieMap(): Map<String, String> = (servletRequest.cookies ?: arrayOf<Cookie>()).map { it.name to it.value }.toMap()

    fun header(header: String): String? = servletRequest.getHeader(header)

    fun headerMap(): Map<String, String> = servletRequest.headerNames.asSequence().map { it to servletRequest.getHeader(it) }.toMap()

    fun host(): String? = servletRequest.getHeader("host")

    fun ip(): String = servletRequest.remoteAddr

    fun path(): String? = servletRequest.pathInfo

    fun port(): Int = servletRequest.serverPort

    fun protocol(): String = servletRequest.protocol

    fun queryParam(queryParam: String): String? = servletRequest.getParameter(queryParam)

    fun queryParamOrDefault(queryParam: String, defaultValue: String): String = servletRequest.getParameter(queryParam) ?: defaultValue

    fun queryParams(queryParam: String): Array<String>? = servletRequest.getParameterValues(queryParam)

    fun queryParamMap(): Map<String, Array<String>> = servletRequest.parameterMap

    fun queryString(): String? = servletRequest.queryString

    fun method(): String = servletRequest.method

    fun scheme(): String = servletRequest.scheme

    fun uri(): String = servletRequest.requestURI

    fun url(): String = servletRequest.requestURL.toString()

    fun userAgent(): String? = servletRequest.getHeader("user-agent")

    //
    // Response-ish methods
    //

    fun response(): HttpServletResponse = servletResponse

    fun result(resultString: String): Context {
        this.resultString = resultString
        this.resultStream = null // can only have one or the other
        return this
    }

    fun resultString(): String? = resultString

    fun resultStream(): InputStream? = resultStream

    fun result(resultStream: InputStream): Context {
        this.resultString = null // can only have one or the other
        this.resultStream = resultStream
        return this
    }

    fun charset(charset: String): Context {
        servletResponse.characterEncoding = charset
        return this
    }

    fun contentType(contentType: String): Context {
        servletResponse.contentType = contentType
        return this
    }

    fun header(headerName: String, headerValue: String): Context {
        servletResponse.setHeader(headerName, headerValue)
        return this
    }

    fun html(html: String): Context = result(html).contentType("text/html")

    fun redirect(location: String) {
        try {
            servletResponse.sendRedirect(location)
        } catch (e: IOException) {
            log.warn("Exception while trying to redirect response", e)
        }
    }

    fun redirect(location: String, httpStatusCode: Int) {
        servletResponse.status = httpStatusCode
        redirect(location)
    }

    fun status(): Int = servletResponse.status

    fun status(statusCode: Int): Context {
        servletResponse.status = statusCode
        return this
    }

    // cookie methods

    fun cookie(name: String, value: String): Context = cookie(CookieBuilder.cookieBuilder(name, value))

    fun cookie(name: String, value: String, maxAge: Int): Context = cookie(CookieBuilder.cookieBuilder(name, value).maxAge(maxAge))

    fun cookie(cookieBuilder: CookieBuilder): Context {
        val cookie = Cookie(cookieBuilder.name, cookieBuilder.value)
        cookie.path = cookieBuilder.path
        cookie.domain = cookieBuilder.domain
        cookie.maxAge = cookieBuilder.maxAge
        cookie.secure = cookieBuilder.secure
        cookie.isHttpOnly = cookieBuilder.httpOnly
        servletResponse.addCookie(cookie)
        return this
    }

    fun removeCookie(name: String): Context = removeCookie(null, name)

    fun removeCookie(path: String?, name: String): Context {
        val cookie = Cookie(name, "")
        cookie.path = path
        cookie.maxAge = 0
        servletResponse.addCookie(cookie)
        return this
    }

    // Translator methods
    // TODO: Consider moving rendering to JavalinServlet, where response is written
    fun json(`object`: Any): Context {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return result(Jackson.toJson(`object`)).contentType("application/json")
    }

    fun renderVelocity(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return html(Velocity.render(templatePath, model))
    }

    fun renderFreemarker(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return html(Freemarker.render(templatePath, model))
    }

    fun renderThymeleaf(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return html(Thymeleaf.render(templatePath, model))
    }

    fun renderMustache(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        return html(Mustache.render(templatePath, model))
    }

}
