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
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.CompletionStage
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Provides access to functions for handling the request and response
 *
 * @see <a href="https://javalin.io/documentation#context">Context in docs</a>
 */
class Context(private val servletResponse: HttpServletResponse,
              private val servletRequest: HttpServletRequest,
              internal var matchedPath: String,
              internal var paramMap: Map<String, String>,
              internal var splatList: List<String>) {

    private var passedToNextHandler: Boolean = false

    private var resultStream: InputStream? = null

    private val cookieStore = CookieStoreUtil.stringToMap(cookie(CookieStoreUtil.name))

    /**
     * Gets cookie store value for specified key.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> cookieStore(key: String): T = cookieStore[key] as T

    /**
     * Sets cookie store value for specified key.
     * Values are made available for other handlers, requests, and servers.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun cookieStore(key: String, value: Any) {
        cookieStore[key] = value
        cookie(CookieStoreUtil.name, CookieStoreUtil.mapToString(cookieStore))
    }

    /**
     * Clears cookie store in the context and from the response.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun clearCookieStore() {
        cookieStore.clear()
        removeCookie(CookieStoreUtil.name)
    }

    /**
     * Instructs Javalin to try and match the request again to
     * the next matching endpoint-handler.
     */
    fun next() {
        passedToNextHandler = true
    }

    /**
     * Checks if [next] has been called on the context.
     */
    fun nexted(): Boolean = passedToNextHandler

    ///////////////////////////////////////////////////////////////
    // Request-ish methods
    ///////////////////////////////////////////////////////////////

    /**
     * Gets the underlying HttpServletRequest
     */
    fun request(): HttpServletRequest = servletRequest

    /**
     * Runs the request asynchronously.
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

    /**
     * Gets the request body as a String.
     */
    fun body(): String = bodyAsBytes().toString(Charset.forName(servletRequest.characterEncoding ?: "UTF-8"))

    /**
     * Gets the request body as a ByteArray.
     */
    fun bodyAsBytes(): ByteArray = servletRequest.inputStream.readBytes()

    /**
     * Maps a JSON body to a Java object using Jackson ObjectMapper.
     * @return The mapped object
     * Requires Jackson library in the classpath.
     */
    fun <T> bodyAsClass(clazz: Class<T>): T {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return JavalinJacksonPlugin.toObject(body(), clazz)
    }

    /**
     * Gets first uploaded file for the specified name.
     * Requires Apache commons-fileupload library in the classpath.
     */
    fun uploadedFile(fileName: String): UploadedFile? = uploadedFiles(fileName).firstOrNull()

    /**
     * Gets a list of uploaded files for the specified name.
     * Requires Apache commons-fileupload library in the classpath.
     */
    fun uploadedFiles(fileName: String): List<UploadedFile> {
        return if (isMultipartFormData()) UploadUtil.getUploadedFiles(servletRequest, fileName) else listOf()
    }

    /**
     * Gets a form param for the specified key from the request
     */
    fun formParam(formParam: String): String? = formParams(formParam)?.get(0)

    /**
     * Gets a form param if it exists, else a default value.
     * This method is mainly useful when calling from Java,
     * use elvis (formParam(key) ?: default) instead in Kotlin.
     */
    fun formParamOrDefault(formParam: String, defaultValue: String): String = formParam(formParam) ?: defaultValue

    /**
     * Gets a list of form params for the specified key.
     */
    fun formParams(formParam: String): Array<String>? = formParamMap()[formParam]

    /**
     * Gets a map with all the form param keys and values.
     */
    fun formParamMap(): Map<String, Array<String>> = if (isMultipartFormData()) mapOf() else ContextUtil.splitKeyValueStringAndGroupByKey(body())

    /**
     * Maps form params to values, or returns null if any of the params are null.
     * Ex: val (username, email) = ctx.mapFormParams("username", "email") ?: throw MissingFormParamException()
     * This method is mainly useful when calling from Kotlin.
     */
    fun mapFormParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { formParam(it) }

    /**
     * Returns true if any of the specified form params are null.
     * Mainly useful when calling from Java as a replacement for [mapFormParams].
     */
    fun anyFormParamNull(vararg keys: String): Boolean = keys.any { formParam(it) == null }

    /**
     * Gets a param by name (ex: param("param").
     *
     * Ex: If the handler path is /users/:user-id,
     * and a browser GETs /users/123,
     * param("user-id") will return "123"
     */
    fun param(param: String): String? = paramMap[":" + param.toLowerCase().replaceFirst(":", "")]

    /**
     * Gets a map of all the [param] keys and values.
     */
    fun paramMap(): Map<String, String> = paramMap.toMap()

    //
    // Gets a splat by its index.
    // Ex: If the handler path is /users/*
    // and a browser GETs /users/123,
    // splat(0) will return "123"
    //
    fun splat(splatNr: Int): String? = splatList[splatNr]

    /**
     * Gets an array of all the [splat] values.
     */
    fun splats(): Array<String> = splatList.toTypedArray()

    /**
     * Gets basic-auth credentials from the request.
     *
     * Returns a wrapper object [BasicAuthCredentials] which contains the
     * Base64 decoded username and password from the Authorization header.
     */
    fun basicAuthCredentials(): BasicAuthCredentials? = ContextUtil.getBasicAuthCredentials(header(Header.AUTHORIZATION))

    /**
     * Sets an attribute on the request, which will be made available to
     * other handlers in the request lifecycle
     */
    fun attribute(attribute: String, value: Any) = servletRequest.setAttribute(attribute, value)

    /**
     * Gets the specified attribute from the request.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(attribute: String): T = servletRequest.getAttribute(attribute) as T

    /**
     * Gets a map with all the attribute keys and values on the request.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> attributeMap(): Map<String, T> = servletRequest.attributeNames.asSequence().map { it to servletRequest.getAttribute(it) as T }.toMap()

    /**
     * Gets the request content length.
     */
    fun contentLength(): Int = servletRequest.contentLength

    /**
     * Gets the request content type.
     */
    fun contentType(): String? = servletRequest.contentType

    /**
     * Gets a request cookie by name.
     */
    fun cookie(name: String): String? = (servletRequest.cookies ?: arrayOf<Cookie>()).find { it.name == name }?.value

    /**
     * Gets a map with all the cookie keys and values on the request.
     */
    fun cookieMap(): Map<String, String> = (servletRequest.cookies ?: arrayOf<Cookie>()).map { it.name to it.value }.toMap()

    /**
     * Gets a request header by name.
     */
    fun header(header: String): String? = servletRequest.getHeader(header)

    /**
     * Gets a map with all the header keys and values on the request.
     */
    fun headerMap(): Map<String, String> = servletRequest.headerNames.asSequence().map { it to servletRequest.getHeader(it) }.toMap()

    /**
     * Gets the request host.
     */
    fun host(): String? = servletRequest.getHeader(Header.HOST)

    /**
     * Gets the request ip.
     */
    fun ip(): String = servletRequest.remoteAddr

    /**
     * Returns true if request is multipart.
     */
    fun isMultipart(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/")

    /**
     * Returns true if request is multipart/form-data.
     */
    fun isMultipartFormData(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/form-data")

    /**
     * Gets the path that Javalin used to match the request.
     *
     * Ex: If the handler path is /users/:user-id,
     * and a browser GETs /users/123,
     * matchedPath() will return /users/:user-id
     */
    fun matchedPath() = matchedPath

    /**
     * Gets the request method.
     */
    fun method(): String = servletRequest.method

    /**
     * Gets the request path.
     */
    fun path(): String = servletRequest.requestURI

    /**
     * Gets the request port.
     */
    fun port(): Int = servletRequest.serverPort

    /**
     * Gets the request protocol.
     */
    fun protocol(): String = servletRequest.protocol

    /**
     * Gets a query param for the specified key.
     */
    fun queryParam(queryParam: String): String? = queryParams(queryParam)?.get(0)

    /**
     * Gets a query param if it exists, else a default value.
     * This method is mainly useful when calling from Java,
     * use elvis (queryParam(key) ?: default) instead in Kotlin.
     */
    fun queryParamOrDefault(queryParam: String, defaultValue: String): String = queryParam(queryParam) ?: defaultValue

    /**
     * Gets a list of query params for the specified key.
     */
    fun queryParams(queryParam: String): Array<String>? = queryParamMap()[queryParam]

    /**
     * Gets a map with all the query param keys and values.
     */
    fun queryParamMap(): Map<String, Array<String>> = ContextUtil.splitKeyValueStringAndGroupByKey(queryString() ?: "")

    /**
     * Maps query params to values, or returns null if any of the params are null.
     * Ex: val (username, email) = ctx.mapQueryParams("username", "email") ?: throw MissingQueryParamException()
     * This method is mainly useful when calling from Kotlin.
     */
    fun mapQueryParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { queryParam(it) }

    /**
     * Returns true if any of the specified query params are null.
     * Mainly useful when calling from Java as a replacement for [mapQueryParams]
     */
    fun anyQueryParamNull(vararg keys: String): Boolean = keys.any { queryParam(it) == null }

    /**
     * Gets the request query string.
     */
    fun queryString(): String? = servletRequest.queryString

    /**
     * Gets the request scheme.
     */
    fun scheme(): String = servletRequest.scheme

    /**
     * Sets a session attribute on the request, which will be made available to
     * other handlers in the session lifecycle.
     */
    fun sessionAttribute(attribute: String, value: Any) = servletRequest.session.setAttribute(attribute, value)

    /**
     * Gets a specific session attribute from the request.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(attribute: String): T = servletRequest.session.getAttribute(attribute) as T

    /**
     * Gets a map of all the session attributes on the request.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttributeMap(): Map<String, T> = servletRequest.session.attributeNames.asSequence().map { it to servletRequest.session.getAttribute(it) as T }.toMap()

    /**
     * Gets the request uri.
     */
    fun uri(): String = servletRequest.requestURI

    /**
     * Gets the request url.
     */
    fun url(): String = servletRequest.requestURL.toString()

    /**
     * Gets the request user agent.
     */
    fun userAgent(): String? = servletRequest.getHeader(Header.USER_AGENT)

    ///////////////////////////////////////////////////////////////
    // Response-ish methods
    ///////////////////////////////////////////////////////////////

    /**
     * Gets the underlying HttpServletResponse.
     */
    fun response(): HttpServletResponse = servletResponse

    /**
     * Sets context result to the specified String.
     * Will overwrite the current result if there is one.
     */
    fun result(resultString: String): Context {
        resultStream = resultString.byteInputStream(stringCharset())
        return this
    }

    /**
     * Gets the current context result as a String (if set).
     */
    fun resultString(): String? {
        val string = resultStream?.readBytes()?.toString(stringCharset())
        resultStream?.reset()
        return string
    }

    /**
     * Sets context result to the specified InputStream.
     * Will overwrite the current result if there is one.
     */
    fun result(resultStream: InputStream): Context {
        this.resultStream = resultStream
        return this
    }

    /**
     * Gets the current context result as an InputStream (if set).
     */
    fun resultStream(): InputStream? = resultStream

    /**
     * Sets response charset to specified value.
     */
    fun charset(charset: String): Context {
        servletResponse.characterEncoding = charset
        return this
    }

    /**
     * Gets the current response charset.
     */
    private fun stringCharset() = try {
        Charset.forName(servletResponse.characterEncoding)
    } catch (e: Exception) {
        Charset.defaultCharset()
    }

    /**
     * Sets response content type to specified value.
     */
    fun contentType(contentType: String): Context {
        servletResponse.contentType = contentType
        return this
    }

    /**
     * Sets response header by name and value.
     */
    fun header(headerName: String, headerValue: String): Context {
        servletResponse.setHeader(headerName, headerValue)
        return this
    }

    /**
     * Sets the response status code and redirects to the specified location.
     */
    @JvmOverloads
    fun redirect(location: String, httpStatusCode: Int = HttpServletResponse.SC_MOVED_TEMPORARILY) {
        servletResponse.setHeader(Header.LOCATION, location)
        throw HaltException(httpStatusCode, "")
    }

    /**
     * Sets the response status.
     */
    fun status(statusCode: Int): Context {
        servletResponse.status = statusCode
        return this
    }

    /**
     * Gets the response status.
     */
    fun status(): Int = servletResponse.status

    /**
     * Sets a cookie with name, value, and (overloaded) max-age.
     */
    @JvmOverloads
    fun cookie(name: String, value: String, maxAge: Int = -1): Context = cookie(CookieBuilder(name, value, maxAge = maxAge))

    /**
     * Sets a cookie using the CookieBuilder.
     */
    fun cookie(cookieBuilder: CookieBuilder): Context {
        val cookie = cookieBuilder.build()
        servletResponse.addCookie(cookie)
        return this
    }

    /**
     * Removes cookie specified by name.
     */
    fun removeCookie(name: String): Context = removeCookie(null, name)

    /**
     * Removes cookie specified by path and name.
     */
    fun removeCookie(path: String?, name: String): Context {
        val cookie = Cookie(name, "")
        cookie.path = path
        cookie.maxAge = 0
        servletResponse.addCookie(cookie)
        return this
    }

    /**
     * Sets context result to specified html string and sets content-type to text/html.
     */
    fun html(html: String): Context = result(html).contentType("text/html")

    /**
     * Serializes object to a JSON-string using Jackson ObjectMapper
     * and sets it as the context result.
     * Sets content type to application/json.
     * Requires Jackson library in the classpath.
     */
    fun json(`object`: Any): Context {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return result(JavalinJacksonPlugin.toJson(`object`)).contentType("application/json")
    }

    /**
     * Renders a Velocity template with specified values as html and
     * sets it as the context result. Sets content-type to text/html.
     * Requires Apache Velocity library in the classpath.
     */
    @JvmOverloads
    fun renderVelocity(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return html(JavalinVelocityPlugin.render(templatePath, model))
    }

    /**
     * Renders a Freemarker template with specified values as html and
     * sets it as the context result. Sets content-type to text/html.
     * Requires Freemarker library in the classpath.
     */
    @JvmOverloads
    fun renderFreemarker(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return html(JavalinFreemarkerPlugin.render(templatePath, model))
    }

    /**
     * Renders a Thymeleaf template with specified values as html and
     * sets it as the context result. Sets content-type to text/html.
     * Requires Thymeleaf library in the classpath.
     */
    @JvmOverloads
    fun renderThymeleaf(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return html(JavalinThymeleafPlugin.render(templatePath, model))
    }

    /**
     * Renders a Mustache template with specified values as html and
     * sets it as the context result. Sets content-type to text/html.
     * Requires Mustache library in the classpath.
     */
    @JvmOverloads
    fun renderMustache(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        return html(JavalinMustachePlugin.render(templatePath, model))
    }

    /**
     * Renders a markdown-file and sets it as the context result.
     * Sets content-type to text/html.
     * Requires Commonmark library in the classpath.
     */
    fun renderMarkdown(markdownFilePath: String): Context {
        Util.ensureDependencyPresent("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "com.atlassian.commonmark/commonmark")
        return html(JavalinCommonmarkPlugin.render(markdownFilePath))
    }

}
