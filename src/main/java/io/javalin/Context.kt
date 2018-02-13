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

    private var resultString: String? = null
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

    /**
     * @return underlying [HttpServletRequest] request object.
     */
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

    /**
     * @return request body as [String].
     */
    fun body(): String = bodyAsBytes().toString(Charset.forName(servletRequest.characterEncoding ?: "UTF-8"))

    /**
     * @return request body as [ByteArray].
     */
    fun bodyAsBytes(): ByteArray = servletRequest.inputStream.readBytes()

    /**
     * @return JSON body to java object using Jackson library.
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

    /**
     * @return single form value for the name.
     */
    fun formParam(formParam: String): String? = formParams(formParam)?.get(0)

    /**
     * @return single form value for the name or default if value is not found.
     */
    fun formParamOrDefault(formParam: String, defaultValue: String): String = formParam(formParam) ?: defaultValue

    /**
     * @return all form values for the name.
     */
    fun formParams(formParam: String): Array<String>? = formParamMap()[formParam]

    /**
     * @return all form key-value pairs as a map.
     */
    fun formParamMap(): Map<String, Array<String>> = if (isMultipartFormData()) mapOf() else ContextUtil.splitKeyValueStringAndGroupByKey(body())

    /**
     * @return form values for given keys.
     */
    fun mapFormParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { formParam(it) }

    /**
     * @return query values for given keys.
     */
    fun mapQueryParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { servletRequest.getParameter(it) }

    /**
     * @return whether form values for given keys is absent.
     */
    fun anyFormParamNull(vararg keys: String): Boolean = keys.any { formParam(it) == null }

    /**
     * @return whether query values for given keys is absent.
     */
    fun anyQueryParamNull(vararg keys: String): Boolean = keys.any { servletRequest.getParameter(it) == null }

    /**
     * @return named param value in the path.
     */
    fun param(param: String): String? = paramMap[":" + param.toLowerCase().replaceFirst(":", "")]

    /**
     * @return values for all named parameters in the path.
     */
    fun paramMap(): Map<String, String> = paramMap.toMap()

    /**
     * @return value for given splat (*) ordinal in the path.
     */
    fun splat(splatNr: Int): String? = splatList[splatNr]

    /**
     * @return values for all splats (*) in the path.
     */
    fun splats(): Array<String> = splatList.toTypedArray()

    // wrapper methods for HttpServletRequest

    /**
     * @return basic credentials used in the request.
     */
    fun basicAuthCredentials(): BasicAuthCredentials? = ContextUtil.getBasicAuthCredentials(header(Header.AUTHORIZATION))

    /**
     * Sets attribute to the current request.
     *
     * @see HttpServletRequest.setAttribute
     */
    fun attribute(attribute: String, value: Any) = servletRequest.setAttribute(attribute, value)

    /**
     * @return attribute from the current request.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(attribute: String): T = servletRequest.getAttribute(attribute) as T

    /**
     * @return all attributes from the current request as a map.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> attributeMap(): Map<String, T> = servletRequest.attributeNames.asSequence().map { it to servletRequest.getAttribute(it) as T }.toMap()

    /**
     * @return content length of the body (in bytes).
     */
    fun contentLength(): Int = servletRequest.contentLength

    /**
     * @return content type of the request.
     */
    fun contentType(): String? = servletRequest.contentType

    /**
     * @return cookie value for given name.
     */
    fun cookie(name: String): String? = (servletRequest.cookies ?: arrayOf<Cookie>()).find { it.name == name }?.value

    /**
     * @return all cookie values in the request as a map.
     */
    fun cookieMap(): Map<String, String> = (servletRequest.cookies ?: arrayOf<Cookie>()).map { it.name to it.value }.toMap()

    /**
     * @return header value for given name.
     */
    fun header(header: String): String? = servletRequest.getHeader(header)

    /**
     * @return all header name-value pairs in the request as a map.
     */
    fun headerMap(): Map<String, String> = servletRequest.headerNames.asSequence().map { it to servletRequest.getHeader(it) }.toMap()

    /**
     * @return value of the Host header.
     */
    fun host(): String? = servletRequest.getHeader(Header.HOST)

    /**
     * @return ip address of the client.
     */
    fun ip(): String = servletRequest.remoteAddr

    /**
     * @return whether the request is multipart.
     */
    fun isMultipart(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/")

    /**
     * @return whether the request is multipart with form data.
     */
    fun isMultipartFormData(): Boolean = (header(Header.CONTENT_TYPE) ?: "").toLowerCase().contains("multipart/form-data")

    /**
     * @return current matched path.
     */
    fun matchedPath() = matchedPath

    /**
     * @return request method.
     */
    fun method(): String = servletRequest.method

    /**
     * @return part of actual URL from protocol name to query string.
     *
     * @see HttpServletRequest.getRequestURI
     */
    fun path(): String = servletRequest.requestURI

    /**
     * @return port number to which request was sent.
     *
     * @see HttpServletRequest.getServerPort
     */
    fun port(): Int = servletRequest.serverPort

    /**
     * @return protocol name and version number
     *
     * @see HttpServletRequest.getProtocol
     */
    fun protocol(): String = servletRequest.protocol

    /**
     * @return query value for given parameter name.
     */
    fun queryParam(queryParam: String): String? = servletRequest.getParameter(queryParam)

    /**
     * @return query value for given parameter name or default value if one does not exists.
     */
    fun queryParamOrDefault(queryParam: String, defaultValue: String): String = servletRequest.getParameter(queryParam) ?: defaultValue

    /**
     * @return all query values for given parameter name.
     */
    fun queryParams(queryParam: String): Array<String>? = servletRequest.getParameterValues(queryParam)

    /**
     * @return all query parameters in the request.
     */
    fun queryParamMap(): Map<String, Array<String>> = servletRequest.parameterMap

    /**
     * @return query string from the request.
     */
    fun queryString(): String? = servletRequest.queryString

    /**
     * @return name of scheme used for the request.
     */
    fun scheme(): String = servletRequest.scheme

    /**
     * Sets attribute with given name and value to the request session.
     */
    fun sessionAttribute(attribute: String, value: Any) = servletRequest.session.setAttribute(attribute, value)

    /**
     * @return attribute value for given name.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(attribute: String): T = servletRequest.session.getAttribute(attribute) as T

    /**
     * @return all attribute name-value pairs in the request as a map.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttributeMap(): Map<String, T> = servletRequest.session.attributeNames.asSequence().map { it to servletRequest.session.getAttribute(it) as T }.toMap()

    /**
     * @return part of actual URL from protocol name to query string.
     *
     * @see HttpServletRequest.getRequestURI
     */
    fun uri(): String = servletRequest.requestURI

    /**
     * @return request url without query parameters.
     *
     * @see HttpServletRequest.getRequestURL
     */
    fun url(): String = servletRequest.requestURL.toString()

    /**
     * @return value of User-Agent header.
     */
    fun userAgent(): String? = servletRequest.getHeader(Header.USER_AGENT)

    //
    // Response-ish methods
    //

    /**
     * @return underlying [HttpServletResponse] response object.
     */
    fun response(): HttpServletResponse = servletResponse

    /**
     * Sets response result to the parameter. The parameter replaces any previous values passed to [result].
     */
    fun result(resultString: String): Context {
        this.resultString = resultString
        this.resultStream = null // can only have one or the other
        return this
    }

    /**
     * @return current response result if string was set or null otherwise.
     */
    fun resultString(): String? = resultString

    /**
     * @return current response result if stream was set or null otherwise.
     */
    fun resultStream(): InputStream? = resultStream

    /**
     * Sets response result to the parameter. The parameter replaces any previous values passed to [result].
     */
    fun result(resultStream: InputStream): Context {
        this.resultString = null // can only have one or the other
        this.resultStream = resultStream
        return this
    }

    /**
     * Sets the charset for the response.
     */
    fun charset(charset: String): Context {
        servletResponse.characterEncoding = charset
        return this
    }

    /**
     * Sets the content type for the response.
     */
    fun contentType(contentType: String): Context {
        servletResponse.contentType = contentType
        return this
    }

    /**
     * Sets header with given name and value in the response.
     */
    fun header(headerName: String, headerValue: String): Context {
        servletResponse.setHeader(headerName, headerValue)
        return this
    }

    /**
     * Sets response result to given html string.
     */
    fun html(html: String): Context = result(html).contentType("text/html")

    /**
     * Sends a temporary redirect response with given location.
     *
     * @see HttpServletResponse.sendRedirect
     */
    fun redirect(location: String) {
        try {
            servletResponse.sendRedirect(location)
        } catch (e: IOException) {
            log.warn("Exception while trying to redirect response", e)
        }
    }

    /**
     * Sets the response status code and redirects to given location.
     *
     * @see HttpServletResponse.sendRedirect
     */
    fun redirect(location: String, httpStatusCode: Int) {
        servletResponse.status = httpStatusCode
        servletResponse.setHeader(Header.LOCATION, location)
    }

    /**
     * @return the response status.
     */
    fun status(): Int = servletResponse.status

    /**
     * Sets the response status to given value.
     */
    fun status(statusCode: Int): Context {
        servletResponse.status = statusCode
        return this
    }

    // cookie methods

    /**
     * Sets cookie with given name and value.
     */
    fun cookie(name: String, value: String): Context = cookie(CookieBuilder(name, value))

    /**
     * Sets cookie with given name, value and maximum age.
     */
    fun cookie(name: String, value: String, maxAge: Int): Context = cookie(CookieBuilder(name, value, maxAge = maxAge))

    /**
     * Sets cookie from given [CookieBuilder] builder.
     */
    fun cookie(cookieBuilder: CookieBuilder): Context {
        val cookie = cookieBuilder.build()
        servletResponse.addCookie(cookie)
        return this
    }

    /**
     * Remove cookie with given key.
     */
    fun removeCookie(name: String): Context = removeCookie(null, name)

    /**
     * Remove cookie with given key from path.
     */
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
     * Render json object as the response result.
     *
     * Requires Jackson library in the classpath.
     */
    fun json(`object`: Any): Context {
        Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
        return result(JavalinJacksonPlugin.toJson(`object`)).contentType("application/json")
    }

    /**
     * Render velocity template with given values as the response result.
     *
     * Requires Apache Velocity library in the classpath.
     */
    fun renderVelocity(templatePath: String, model: Map<String, Any?>): Context {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        return html(JavalinVelocityPlugin.render(templatePath, model))
    }

    /**
     * Render velocity template as the response result.
     *
     * Requires Apache Velocity library in the classpath.
     */
    fun renderVelocity(templatePath: String): Context = renderVelocity(templatePath, mapOf())

    /**
     * Render freemarker template with given values as the response result.
     *
     * Requires Apache Freemarker library in the classpath.
     */
    fun renderFreemarker(templatePath: String, model: Map<String, Any?>): Context {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        return html(JavalinFreemarkerPlugin.render(templatePath, model))
    }

    /**
     * Render freemarker template as the response result.
     *
     * Requires Apache Freemarker library in the classpath.
     */
    fun renderFreemarker(templatePath: String): Context = renderFreemarker(templatePath, mapOf())

    /**
     * Render thymeleaf template with given values as the response result.
     *
     * Requires thymeleaf library in the classpath.
     */
    fun renderThymeleaf(templatePath: String, model: Map<String, Any?>): Context {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        return html(JavalinThymeleafPlugin.render(templatePath, model))
    }

    /**
     * Render thymeleaf template as the response result.
     *
     * Requires thymeleaf library in the classpath.
     */
    fun renderThymeleaf(templatePath: String): Context = renderThymeleaf(templatePath, mapOf())

    /**
     * Render mustache template with given values as the response result.
     *
     * Requires com.github.mustachejava.Mustache library in the classpath.
     */
    fun renderMustache(templatePath: String, model: Map<String, Any?>): Context {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        return html(JavalinMustachePlugin.render(templatePath, model))
    }

    /**
     * Render mustache template as the response result.
     *
     * Requires com.github.mustachejava.Mustache library in the classpath.
     */
    fun renderMustache(templatePath: String): Context = renderMustache(templatePath, mapOf())

    /**
     * Render markdown template with given values as the response result.
     *
     * Requires commonmark markdown library in the classpath.
     */
    fun renderMarkdown(markdownFilePath: String): Context {
        Util.ensureDependencyPresent("Commonmark", "org.commonmark.renderer.html.HtmlRenderer", "com.atlassian.commonmark/commonmark")
        return html(JavalinCommonmarkPlugin.render(markdownFilePath))
    }

}
