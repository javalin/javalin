/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.builder.CookieBuilder
import io.javalin.core.util.*
import io.javalin.translator.json.JavalinJacksonPlugin
import io.javalin.translator.markdown.JavalinCommonmarkPlugin
import io.javalin.translator.template.JavalinFreemarkerPlugin
import io.javalin.translator.template.JavalinMustachePlugin
import io.javalin.translator.template.JavalinThymeleafPlugin
import io.javalin.translator.template.JavalinVelocityPlugin
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.Charset
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

    private val cookieStore = CookieStoreUtil.stringToMap(cookie(CookieStoreUtil.name))

    @Suppress("UNCHECKED_CAST")
    fun <T> cookieStore(key: String): T = cookieStore[key] as T

    fun cookieStore(key: String, value: Any) {
        cookieStore[key] = value
        cookie(CookieStoreUtil.name, CookieStoreUtil.mapToString(cookieStore))
    }

    fun clearCookieStore() {
        cookieStore.clear();
        removeCookie(CookieStoreUtil.name)
    }

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

    fun body(): String = bodyAsBytes().toString(Charset.forName(servletRequest.characterEncoding ?: "UTF-8"))

    fun bodyAsBytes(): ByteArray = servletRequest.inputStream.readBytes()

    fun <T> bodyAsClass(clazz: Class<T>): T {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return JavalinJacksonPlugin.toObject(body(), clazz)
    }

    fun uploadedFile(fileName: String): UploadedFile? = uploadedFiles(fileName).firstOrNull()

    fun uploadedFiles(fileName: String): List<UploadedFile> {
        Util.ensureDependencyPresent("FileUpload", "org.apache.commons.fileupload.servlet.ServletFileUpload", "commons-fileupload/commons-fileupload")
        return UploadUtil.getUploadedFiles(servletRequest, fileName)
    }

    fun formParam(formParam: String): String? = formParams(formParam)?.get(0)

    fun formParamOrDefault(formParam: String, defaultValue: String): String = formParam(formParam) ?: defaultValue

    fun formParams(formParam: String): Array<String>? = formParamMap()[formParam]

    fun formParamMap(): Map<String, Array<String>> {
        if (isMultipart()) return mapOf()
        return body().split("&").map { it.split("=") }.groupBy(
                { it[0] },
                { if (it.size > 1) URLDecoder.decode(it[1], "UTF-8") else "" }
        ).mapValues { it.value.toTypedArray() }
    }

    fun mapFormParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { formParam(it) }

    fun mapQueryParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { servletRequest.getParameter(it) }

    fun anyFormParamNull(vararg keys: String): Boolean = keys.filter { formParam(it) == null }.isNotEmpty()

    fun anyQueryParamNull(vararg keys: String): Boolean = keys.filter { servletRequest.getParameter(it) == null }.isNotEmpty()

    fun param(param: String): String? = paramMap[":" + param.toLowerCase().replaceFirst(":", "")]

    fun paramMap(): Map<String, String> = paramMap.toMap()

    fun splat(splatNr: Int): String? = splatList[splatNr]

    fun splats(): Array<String> = splatList.toTypedArray()

    // wrapper methods for HttpServletRequest

    fun basicAuthCredentials(): BasicAuthCredentials? = ContextUtil.getBasicAuthCredentials(header(Header.AUTHORIZATION))

    fun attribute(attribute: String, value: Any) = servletRequest.setAttribute(attribute, value)

    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(attribute: String): T = servletRequest.getAttribute(attribute) as T

    @Suppress("UNCHECKED_CAST")
    fun <T> attributeMap(): Map<String, T> = servletRequest.attributeNames.asSequence().map { it to servletRequest.getAttribute(it) as T }.toMap()

    fun contentLength(): Int = servletRequest.contentLength

    fun contentType(): String? = servletRequest.contentType

    fun cookie(name: String): String? = (servletRequest.cookies ?: arrayOf<Cookie>()).find { it.name == name }?.value

    fun cookieMap(): Map<String, String> = (servletRequest.cookies ?: arrayOf<Cookie>()).map { it.name to it.value }.toMap()

    fun header(header: String): String? = servletRequest.getHeader(header)

    fun headerMap(): Map<String, String> = servletRequest.headerNames.asSequence().map { it to servletRequest.getHeader(it) }.toMap()

    fun host(): String? = servletRequest.getHeader(Header.HOST)

    fun ip(): String = servletRequest.remoteAddr

    fun isMultipart(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/form-data")

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

    fun userAgent(): String? = servletRequest.getHeader(Header.USER_AGENT)

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
        return result(JavalinJacksonPlugin.toJson(`object`)).contentType("application/json")
    }

    fun renderVelocity(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return html(JavalinVelocityPlugin.render(templatePath, model))
    }

    fun renderVelocity(templatePath: String): Context = renderVelocity(templatePath, mapOf())

    fun renderFreemarker(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return html(JavalinFreemarkerPlugin.render(templatePath, model))
    }

    fun renderFreemarker(templatePath: String): Context = renderFreemarker(templatePath, mapOf())

    fun renderThymeleaf(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return html(JavalinThymeleafPlugin.render(templatePath, model))
    }

    fun renderThymeleaf(templatePath: String): Context = renderThymeleaf(templatePath, mapOf())

    fun renderMustache(templatePath: String, model: Map<String, Any>): Context {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        return html(JavalinMustachePlugin.render(templatePath, model))
    }

    fun renderMustache(templatePath: String): Context = renderMustache(templatePath, mapOf())

    fun renderMarkdown(markdownFilePath: String): Context {
        Util.ensureDependencyPresent("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "com.atlassian.commonmark/commonmark")
        return html(JavalinCommonmarkPlugin.render(markdownFilePath))
    }

}
