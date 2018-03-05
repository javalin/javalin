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
     * Provides cookie store values for specified key.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> cookieStore(key: String): T = cookieStore[key] as T

    /**
     * Sets cookie store value for specified key.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun cookieStore(key: String, value: Any) {
        cookieStore[key] = value
        cookie(CookieStoreUtil.name, CookieStoreUtil.mapToString(cookieStore))
    }

    /**
     * Clears cookie store in the request and context.
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

    // gets the underlying HttpServletRequest
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

    // gets the request body as a String
    fun body(): String = bodyAsBytes().toString(Charset.forName(servletRequest.characterEncoding ?: "UTF-8"))

    // gets the request body as a ByteArray
    fun bodyAsBytes(): ByteArray = servletRequest.inputStream.readBytes()

    /**
     * Maps a JSON body to a Java object using Jackson ObjectMapper
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
     * Gets a list of uploaded files for the specified name
     * Requires Apache commons-fileupload library in the classpath.
     */
    fun uploadedFiles(fileName: String): List<UploadedFile> {
        Util.ensureDependencyPresent("FileUpload", "org.apache.commons.fileupload.servlet.ServletFileUpload", "commons-fileupload/commons-fileupload")
        return UploadUtil.getUploadedFiles(servletRequest, fileName)
    }

    // gets a form param for the specified key
    fun formParam(formParam: String): String? = formParams(formParam)?.get(0)

    // gets a form param if it exists, else a default value (mainly useful when calling from Java)
    fun formParamOrDefault(formParam: String, defaultValue: String): String = formParam(formParam) ?: defaultValue

    // gets a list of form params for the specified key
    fun formParams(formParam: String): Array<String>? = formParamMap()[formParam]

    // gets a map with all the form param keys and values
    fun formParamMap(): Map<String, Array<String>> = if (isMultipartFormData()) mapOf() else ContextUtil.splitKeyValueStringAndGroupByKey(body())

    // maps form params to values or returns null if any of the params are null
    // ex: val (username, email) = ctx.mapFormParams("username", "email") ?: throw MissingFormParamException()
    // mainly useful when calling from Kotlin
    fun mapFormParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { formParam(it) }

    // returns true if any of the form params specified are null
    // mainly useful when calling from Java
    fun anyFormParamNull(vararg keys: String): Boolean = keys.any { formParam(it) == null }

    // gets a param by name (ex: param("param") for url /path/:param
    fun param(param: String): String? = paramMap[":" + param.toLowerCase().replaceFirst(":", "")]

    // gets a map of all dynamic path keys and values the request
    fun paramMap(): Map<String, String> = paramMap.toMap()

    // gets a splat by index
    fun splat(splatNr: Int): String? = splatList[splatNr]

    // gets all splat-values for the request
    // see https://javalin.io/documentation#endpoint-handlers for details
    fun splats(): Array<String> = splatList.toTypedArray()

    /**
     * Gets basic-auth credentials from the request
     *
     * Returns a wrapper object [BasicAuthCredentials] which contains the
     * Base64 decoded username and password from the Authorization header
     */
    fun basicAuthCredentials(): BasicAuthCredentials? = ContextUtil.getBasicAuthCredentials(header(Header.AUTHORIZATION))

    // sets an attribute on the request
    fun attribute(attribute: String, value: Any) = servletRequest.setAttribute(attribute, value)

    // gets a specific attribute from the request
    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(attribute: String): T = servletRequest.getAttribute(attribute) as T

    // gets a map with all the attribute keys and values on the request
    @Suppress("UNCHECKED_CAST")
    fun <T> attributeMap(): Map<String, T> = servletRequest.attributeNames.asSequence().map { it to servletRequest.getAttribute(it) as T }.toMap()

    // gets the request content length
    fun contentLength(): Int = servletRequest.contentLength

    // gets the request content type
    fun contentType(): String? = servletRequest.contentType

    // gets a request cookie by name
    fun cookie(name: String): String? = (servletRequest.cookies ?: arrayOf<Cookie>()).find { it.name == name }?.value

    // gets a map with all the cookie keys and values on the request
    fun cookieMap(): Map<String, String> = (servletRequest.cookies ?: arrayOf<Cookie>()).map { it.name to it.value }.toMap()

    // gets a header for the specified key from the request
    fun header(header: String): String? = servletRequest.getHeader(header)

    // gets a map with all the header keys and values on the request
    fun headerMap(): Map<String, String> = servletRequest.headerNames.asSequence().map { it to servletRequest.getHeader(it) }.toMap()

    // gets the request host
    fun host(): String? = servletRequest.getHeader(Header.HOST)

    // gets the request ip
    fun ip(): String = servletRequest.remoteAddr

    // returns true if request is multipart
    fun isMultipart(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/")

    // returns true if request is multipart/form-data
    fun isMultipartFormData(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/form-data")

    // gets the path that Javalin used to match the request (ex "/users/:user-id" for "/users/f8-54hq")
    fun matchedPath() = matchedPath

    // gets the request method
    fun method(): String = servletRequest.method

    // gets the request path
    fun path(): String = servletRequest.requestURI

    // gets the request port
    fun port(): Int = servletRequest.serverPort

    // gets the request protocol
    fun protocol(): String = servletRequest.protocol

    // gets a query param for the specified key
    fun queryParam(queryParam: String): String? = queryParams(queryParam)?.get(0)

    // gets a query param if it exists, else a default value (mainly useful when calling from Java)
    fun queryParamOrDefault(queryParam: String, defaultValue: String): String = queryParam(queryParam) ?: defaultValue

    // gets a list of query params for the specified key
    fun queryParams(queryParam: String): Array<String>? = queryParamMap()[queryParam]

    // gets a map with all the query param keys and values
    fun queryParamMap(): Map<String, Array<String>> = ContextUtil.splitKeyValueStringAndGroupByKey(queryString() ?: "")

    // maps query params to values or returns null if any of the params are null
    // ex: val (username, email) = ctx.mapQueryParams("username", "email") ?: throw MissingQueryParamException()
    // mainly useful when calling from Kotlin
    fun mapQueryParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { queryParam(it) }

    // returns true if any of the query params specified are null
    // mainly useful when calling from Java
    fun anyQueryParamNull(vararg keys: String): Boolean = keys.any { queryParam(it) == null }

    // gets the request query string
    fun queryString(): String? = servletRequest.queryString

    // gets the request scheme
    fun scheme(): String = servletRequest.scheme

    // sets a session attribute on the request
    fun sessionAttribute(attribute: String, value: Any) = servletRequest.session.setAttribute(attribute, value)

    // gets a specific session attribute from the request
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(attribute: String): T = servletRequest.session.getAttribute(attribute) as T

    // gets a map of all the session attributes on the request
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttributeMap(): Map<String, T> = servletRequest.session.attributeNames.asSequence().map { it to servletRequest.session.getAttribute(it) as T }.toMap()

    // gets the request uri
    fun uri(): String = servletRequest.requestURI

    // gets the request url
    fun url(): String = servletRequest.requestURL.toString()

    // gets the request user agent (ex: "Chrome/51.0.2704.106")
    fun userAgent(): String? = servletRequest.getHeader(Header.USER_AGENT)

    ///////////////////////////////////////////////////////////////
    // Response-ish methods
    ///////////////////////////////////////////////////////////////

    // gets the underlying HttpServletResponse
    fun response(): HttpServletResponse = servletResponse

    /**
     * Sets context result to the specified String.
     * Will overwrite the current result if there is one
     */
    fun result(resultString: String): Context {
        resultStream = resultString.byteInputStream(stringCharset())
        return this
    }

    // gets the current context result as a String (if set)
    fun resultString(): String? {
        val string = resultStream?.readBytes()?.toString(stringCharset())
        resultStream?.reset()
        return string
    }

    // gets the current response charset
    private fun stringCharset() = try {
        Charset.forName(servletResponse.characterEncoding)
    } catch (e: Exception) {
        Charset.defaultCharset()
    }

    // gets the current context result as a InputStream (if set)
    fun resultStream(): InputStream? = resultStream

    /**
     * Sets context result to the specified InputStream.
     * Will overwrite the current result if there is one
     */
    fun result(resultStream: InputStream): Context {
        this.resultStream = resultStream
        return this
    }

    // sets response charset to specified value
    fun charset(charset: String): Context {
        servletResponse.characterEncoding = charset
        return this
    }

    // sets response content type to specified value
    fun contentType(contentType: String): Context {
        servletResponse.contentType = contentType
        return this
    }

    // sets response header by name and value
    fun header(headerName: String, headerValue: String): Context {
        servletResponse.setHeader(headerName, headerValue)
        return this
    }

    /**
     * Sets context result to specified html string
     * and sets content-type to text/html
     */
    fun html(html: String): Context = result(html).contentType("text/html")

    /**
     * Sets the response status code and
     * redirects to the specified location.
     */
    @JvmOverloads
    fun redirect(location: String, httpStatusCode: Int = HttpServletResponse.SC_MOVED_TEMPORARILY) {
        servletResponse.setHeader(Header.LOCATION, location)
        throw HaltException(httpStatusCode, "")
    }

    // gets the response status
    fun status(): Int = servletResponse.status

    // sets the response status
    fun status(statusCode: Int): Context {
        servletResponse.status = statusCode
        return this
    }

    // sets a cookie with name, value, and (overloaded) max-age
    @JvmOverloads
    fun cookie(name: String, value: String, maxAge: Int = -1): Context = cookie(CookieBuilder(name, value, maxAge = maxAge))

    // sets a cookie using the CookieBuilder
    fun cookie(cookieBuilder: CookieBuilder): Context {
        val cookie = cookieBuilder.build()
        servletResponse.addCookie(cookie)
        return this
    }

    // removes cookie specified by name
    fun removeCookie(name: String): Context = removeCookie(null, name)

    // removes cookie specified by path and name
    fun removeCookie(path: String?, name: String): Context {
        val cookie = Cookie(name, "")
        cookie.path = path
        cookie.maxAge = 0
        servletResponse.addCookie(cookie)
        return this
    }

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
     * Renders a Velocity template with specified values as html
     * and sets it as the context result. Sets content-type to text/html.
     * Requires Apache Velocity library in the classpath.
     */
    @JvmOverloads
    fun renderVelocity(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return html(JavalinVelocityPlugin.render(templatePath, model))
    }

    /**
     * Renders a Freemarker template with specified values as html
     * and sets it as the context result. Sets content-type to text/html.
     * Requires Freemarker library in the classpath.
     */
    @JvmOverloads
    fun renderFreemarker(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return html(JavalinFreemarkerPlugin.render(templatePath, model))
    }

    /**
     * Renders a Thymeleaf template with specified values as html
     * and sets it as the context result. Sets content-type to text/html.
     * Requires Thymeleaf library in the classpath.
     */
    @JvmOverloads
    fun renderThymeleaf(templatePath: String, model: Map<String, Any?> = emptyMap()): Context {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return html(JavalinThymeleafPlugin.render(templatePath, model))
    }

    /**
     * Renders a Mustache template with specified values as html
     * and sets it as the context result. Sets content-type to text/html.
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
