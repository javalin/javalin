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
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.CompletionStage
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Provides access to request and response
 *
 * @see <a href="https://javalin.io/documentation#context">Context in docs</a>
 */
class Context(private val servletResponse: HttpServletResponse,
              private val servletRequest: HttpServletRequest,
              internal var matchedPath: String,
              internal var paramMap: Map<String, String>,
              internal var splatList: List<String>) {

    private val log = LoggerFactory.getLogger(Context::class.java)

    private var passedToNextHandler: Boolean = false

    private var resultStream: InputStream? = null

    private val cookieStore = CookieStoreUtil.stringToMap(cookie(CookieStoreUtil.name))

    /**
     * Provides cookie store values for given key.
     *
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> cookieStore(key: String): T = cookieStore[key] as T

    /**
     * Sets cookie store value for given key.
     *
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun cookieStore(key: String, value: Any) {
        cookieStore[key] = value
        cookie(CookieStoreUtil.name, CookieStoreUtil.mapToString(cookieStore))
    }

    /**
     * Clears cookie store in the request and context.
     *
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun clearCookieStore() {
        cookieStore.clear()
        removeCookie(CookieStoreUtil.name)
    }

    /**
     * Passes request to the next matching handler.
     *
     * @see nexted
     */
    fun next() {
        passedToNextHandler = true
    }

    /**
     * Checks whether the request was passed before.
     *
     * @see next
     */
    fun nexted(): Boolean = passedToNextHandler

    //
    // Request-ish methods
    //

    fun request(): HttpServletRequest = servletRequest

    /**
     * Runs the request asynchronously.
     *
     * @see HttpServletRequest.startAsync
     */
    @Deprecated("This is an experimental feature, it might be removed/reworked later")
    fun async(asyncHandler: () -> CompletionStage<Void>) {
        val asyncContext = servletRequest.startAsync()
        asyncHandler().thenAccept { _ -> asyncContext.complete() }
                .exceptionally { e ->
                    asyncContext.complete()
                    throw RuntimeException(e)
                }
    }

    fun body(): String = bodyAsBytes().toString(Charset.forName(servletRequest.characterEncoding ?: "UTF-8"))

    fun bodyAsBytes(): ByteArray = servletRequest.inputStream.readBytes()

    /**
     * Maps a JSON body to a Java object using Jackson ObjectMapper
     * @return The mapped object
     *
     * Requires Jackson library in the classpath.
     */
    fun <T> bodyAsClass(clazz: Class<T>): T {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return JavalinJacksonPlugin.toObject(body(), clazz)
    }

    /**
     * @return first uploaded file for the name.
     *
     * Requires Apache commons-fileupload library in the classpath.
     */
    fun uploadedFile(fileName: String): UploadedFile? = uploadedFiles(fileName).firstOrNull()

    /**
     * @return list of uploaded files for the name
     *
     * Requires Apache commons-fileupload library in the classpath.
     */
    fun uploadedFiles(fileName: String): List<UploadedFile> {
        Util.ensureDependencyPresent("FileUpload", "org.apache.commons.fileupload.servlet.ServletFileUpload", "commons-fileupload/commons-fileupload")
        return UploadUtil.getUploadedFiles(servletRequest, fileName)
    }

    fun formParam(formParam: String): String? = formParams(formParam)?.get(0)
    fun formParamOrDefault(formParam: String, defaultValue: String): String = formParam(formParam) ?: defaultValue
    fun formParams(formParam: String): Array<String>? = formParamMap()[formParam]
    fun formParamMap(): Map<String, Array<String>> = if (isMultipartFormData()) mapOf() else ContextUtil.splitKeyValueStringAndGroupByKey(body())
    fun mapFormParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { formParam(it) }
    fun anyFormParamNull(vararg keys: String): Boolean = keys.any { formParam(it) == null }

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

    fun isMultipart(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/")

    fun isMultipartFormData(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/form-data")

    fun matchedPath() = matchedPath

    fun method(): String = servletRequest.method

    fun path(): String = servletRequest.requestURI

    fun port(): Int = servletRequest.serverPort

    fun protocol(): String = servletRequest.protocol

    fun queryParam(queryParam: String): String? = queryParams(queryParam)?.get(0)
    fun queryParamOrDefault(queryParam: String, defaultValue: String): String = queryParam(queryParam) ?: defaultValue
    fun queryParams(queryParam: String): Array<String>? = queryParamMap()[queryParam]
    fun queryParamMap(): Map<String, Array<String>> = ContextUtil.splitKeyValueStringAndGroupByKey(queryString() ?: "")
    fun mapQueryParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { queryParam(it) }
    fun anyQueryParamNull(vararg keys: String): Boolean = keys.any { queryParam(it) == null }

    fun queryString(): String? = servletRequest.queryString

    fun scheme(): String = servletRequest.scheme

    fun sessionAttribute(attribute: String, value: Any) = servletRequest.session.setAttribute(attribute, value)

    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(attribute: String): T = servletRequest.session.getAttribute(attribute) as T

    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttributeMap(): Map<String, T> = servletRequest.session.attributeNames.asSequence().map { it to servletRequest.session.getAttribute(it) as T }.toMap()

    fun uri(): String = servletRequest.requestURI

    fun url(): String = servletRequest.requestURL.toString()

    fun userAgent(): String? = servletRequest.getHeader(Header.USER_AGENT)

    //
    // Response-ish methods
    //

    fun response(): HttpServletResponse = servletResponse

    /**
     * Sets response result to the parameter. The parameter replaces any previous values passed to [result].
     */
    fun result(resultString: String): Context {
        resultStream = resultString.byteInputStream(stringCharset())
        return this
    }


    /**
     * @return current response result as string or null otherwise
     */
    fun resultString(): String? {
        val string = resultStream?.readBytes()?.toString(stringCharset())
        resultStream?.reset()
        return string
    }

    private fun stringCharset() = try {
        Charset.forName(servletResponse.characterEncoding)
    } catch (e: Exception) {
        Charset.defaultCharset()
    }

    /**
     * @return current response result as stream or null otherwise.
     */
    fun resultStream(): InputStream? = resultStream

    /**
     * Sets response result to the parameter. The parameter replaces any previous values passed to [result].
     */
    fun result(resultStream: InputStream): Context {
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

    /**
     * Sets response result to given html string.
     *
     * @see result
     */
    fun html(html: String): Context = result(html).contentType("text/html")

    /**
     * Sets the response status code and redirects to given location.
     */
    @JvmOverloads
    fun redirect(location: String, httpStatusCode: Int = HttpServletResponse.SC_MOVED_TEMPORARILY) {

        servletResponse.setHeader(Header.LOCATION, location)
        throw HaltException(httpStatusCode, "")
    }

    fun status(): Int = servletResponse.status

    fun status(statusCode: Int): Context {
        servletResponse.status = statusCode
        return this
    }

    // cookie methods

    @JvmOverloads
    fun cookie(name: String, value: String, maxAge: Int = -1): Context = cookie(CookieBuilder(name, value, maxAge = maxAge))

    fun cookie(cookieBuilder: CookieBuilder): Context {
        val cookie = cookieBuilder.build()
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
    /**
     * Sets content type to application/json and result to the JSON representation of a given object using Jackson ObjectMapper.
     *
     * Requires Jackson library in the classpath.
     */
    fun json(`object`: Any): Context {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return result(JavalinJacksonPlugin.toJson(`object`)).contentType("application/json")
    }

    /**
     * Renders velocity template with given values as html and sets it as the response result.
     *
     * Requires Apache Velocity library in the classpath.
     */
    @JvmOverloads
    fun renderVelocity(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return html(JavalinVelocityPlugin.render(templatePath, model))
    }

    /**
     * Renders freemarker template with given values as html and sets it as the response result.
     *
     * Requires Apache Freemarker library in the classpath.
     */
    @JvmOverloads
    fun renderFreemarker(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return html(JavalinFreemarkerPlugin.render(templatePath, model))
    }

    /**
     * Renders thymeleaf template with given values as html and sets it as the response result.
     *
     * Requires thymeleaf library in the classpath.
     */
    @JvmOverloads
    fun renderThymeleaf(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return html(JavalinThymeleafPlugin.render(templatePath, model))
    }

    /**
     * Renders mustache template with given values as html and sets it as the response result.
     *
     * Requires mustache library in the classpath.
     */
    @JvmOverloads
    fun renderMustache(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        return html(JavalinMustachePlugin.render(templatePath, model))
    }

    /**
     * Renders markdown template as html and sets it as the response result.
     *
     * Requires commonmark markdown library in the classpath.
     */
    fun renderMarkdown(markdownFilePath: String): Context {
        Util.ensureDependencyPresent("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "com.atlassian.commonmark/commonmark")
        return html(JavalinCommonmarkPlugin.render(markdownFilePath))
    }

}
