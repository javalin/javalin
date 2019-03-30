/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.cookie.CookieStore
import io.javalin.core.HandlerType
import io.javalin.core.util.ContextUtil
import io.javalin.core.util.Header
import io.javalin.core.util.MultipartUtil
import io.javalin.json.JavalinJson
import io.javalin.rendering.JavalinRenderer
import io.javalin.validation.TypedValidator
import io.javalin.validation.Validator
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Provides access to functions for handling the request and response
 *
 * @see <a href="https://javalin.io/documentation#context">Context in docs</a>
 */
open class Context(private val servletRequest: HttpServletRequest, private val servletResponse: HttpServletResponse, private val appAttributes: Map<Class<*>, Any>) {

    // @formatter:off
    @get:JvmSynthetic @set:JvmSynthetic internal var inExceptionHandler = false
    @get:JvmSynthetic @set:JvmSynthetic internal var matchedPath = ""
    @get:JvmSynthetic @set:JvmSynthetic internal var endpointHandlerPath = ""
    @get:JvmSynthetic @set:JvmSynthetic internal var pathParamMap = mapOf<String, String>()
    @get:JvmSynthetic @set:JvmSynthetic internal var handlerType = HandlerType.BEFORE
    @JvmField val req = servletRequest
    @JvmField val res = servletResponse
    // @formatter:on

    private val cookieStore by lazy { CookieStore(cookie(CookieStore.COOKIE_NAME)) }
    private var resultStream: InputStream? = null
    private var resultFuture: CompletableFuture<*>? = null

    /** Gets an attribute from the Javalin instance serving the request */
    fun <T> appAttribute(clazz: Class<T>): T = appAttributes[clazz] as T

    /**
     * Gets cookie store value for specified key.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun <T> cookieStore(key: String): T = cookieStore[key]

    /**
     * Sets cookie store value for specified key.
     * Values are made available for other handlers, requests, and servers.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun cookieStore(key: String, value: Any) {
        cookieStore[key] = value
        cookie(cookieStore.serializeToCookie())
    }

    /**
     * Clears cookie store in the context and from the response.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun clearCookieStore() {
        cookieStore.clear()
        removeCookie(CookieStore.COOKIE_NAME)
    }

    ///////////////////////////////////////////////////////////////
    // Request-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the request body as a [String]. */
    fun body(): String = bodyAsBytes().toString(Charset.forName(servletRequest.characterEncoding ?: "UTF-8"))

    /**
     * Maps a JSON body to a Java/Kotlin class using JavalinJson.
     * JavalinJson can be configured to use any mapping library.
     * @return The mapped object
     */
    inline fun <reified T : Any> body(): T = bodyAsClass(T::class.java)

    /** Gets the request body as a [ByteArray]. */
    fun bodyAsBytes(): ByteArray = servletRequest.inputStream.readBytes()

    /**
     * Maps a JSON body to a Java/Kotlin class using JavalinJson.
     * JavalinJson can be configured to use any mapping library.
     * @return The mapped object
     */
    fun <T> bodyAsClass(clazz: Class<T>): T = JavalinJson.fromJson(body(), clazz)

    /**
     * Creates a [TypedValidator] for the body() value, with the prefix "Request body as $clazz"
     * Throws [BadRequestResponse] if validation fails.
     */
    fun <T> bodyValidator(clazz: Class<T>) = try {
        TypedValidator(JavalinJson.fromJson(body(), clazz), "Request body as ${clazz.simpleName}")
    } catch (e: Exception) {
        throw BadRequestResponse("Couldn't deserialize body to ${clazz.simpleName}")
    }

    /** Reified version of [bodyValidator] */
    inline fun <reified T : Any> bodyValidator() = bodyValidator(T::class.java)

    /** Gets first [UploadedFile] for the specified name, or null. */
    fun uploadedFile(fileName: String): UploadedFile? = uploadedFiles(fileName).firstOrNull()

    /** Gets a list of [UploadedFile]s for the specified name, or empty list. */
    fun uploadedFiles(fileName: String): List<UploadedFile> {
        return if (isMultipartFormData()) MultipartUtil.getUploadedFiles(servletRequest, fileName) else listOf()
    }

    /**
     * Gets a form param if it exists, else a default value (null if not specified explicitly).
     * Including a default value is mainly useful when calling from Java,
     * use elvis (formParam(key) ?: default) instead in Kotlin.
     */
    @JvmOverloads
    fun formParam(key: String, default: String? = null): String? = formParams(key).firstOrNull() ?: default

    /**
     * Creates a [TypedValidator] for the formParam() value, with the prefix "Form parameter '$key' with value '$value'"
     * Throws [BadRequestResponse] if validation fails.
     */
    @JvmOverloads
    fun <T> formParam(key: String, clazz: Class<T>, default: String? = null) = Validator(formParam(key, default), "Form parameter '$key' with value '${formParam(key, default)}'").asClass(clazz)

    /** Reified version of [formParam] (Kotlin only) */
    inline fun <reified T : Any> formParam(key: String, default: String? = null) = formParam(key, T::class.java, default)

    /** Gets a list of form params for the specified key, or empty list. */
    fun formParams(key: String): List<String> = formParamMap()[key] ?: emptyList()

    /** Gets a map with all the form param keys and values. */
    fun formParamMap(): Map<String, List<String>> =
            if (isMultipartFormData()) MultipartUtil.getFieldMap(servletRequest)
            else ContextUtil.splitKeyValueStringAndGroupByKey(body())

    /**
     * Gets a path param by name (ex: pathParam("param").
     *
     * Ex: If the handler path is /users/:user-id,
     * and a browser GETs /users/123,
     * pathParam("user-id") will return "123"
     */
    fun pathParam(key: String): String = ContextUtil.pathParamOrThrow(pathParamMap, key, matchedPath)

    /**
     * Creates a [TypedValidator] for the pathParam() value, with the prefix "Path parameter '$key' with value '$value'"
     * Throws [BadRequestResponse] if validation fails.
     */
    fun <T> pathParam(key: String, clazz: Class<T>) = Validator(pathParam(key), "Path parameter '$key' with value '${pathParam(key)}'").asClass(clazz)

    /** Reified version of [pathParam] (Kotlin only) */
    inline fun <reified T : Any> pathParam(key: String) = pathParam(key, T::class.java)

    /** Gets a map of all the [pathParam] keys and values. */
    fun pathParamMap(): Map<String, String> = Collections.unmodifiableMap(pathParamMap)

    /**
     * Gets basic-auth credentials from the request.
     *
     * Returns a wrapper object [BasicAuthCredentials] which contains the
     * Base64 decoded username and password from the Authorization header.
     */
    fun basicAuthCredentials(): BasicAuthCredentials? = ContextUtil.getBasicAuthCredentials(header(Header.AUTHORIZATION))

    /**
     * Registers an extension to the Context, which can be used later in the request-lifecycle.
     * This method is mainly useful when calling from Java, as Kotlin has native extension methods.
     *
     * Ex: ctx.register(MyExt.class, myExtInstance())
     */
    fun register(clazz: Class<*>, value: Any) = servletRequest.setAttribute("ctx-ext-${clazz.canonicalName}", value)

    /**
     * Use an extension stored in the Context.
     * This method is mainly useful when calling from Java as Kotlin has native extension methods.
     *
     * Ex: ctx.use(MyExt.class).myMethod()
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> use(clazz: Class<T>): T = servletRequest.getAttribute("ctx-ext-${clazz.canonicalName}") as T

    /** Sets an attribute on the request. Attributes are available to other handlers in the request lifecycle */
    fun attribute(attribute: String, value: Any?) = servletRequest.setAttribute(attribute, value)

    /** Gets the specified attribute from the request. */
    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(attribute: String): T? = servletRequest.getAttribute(attribute) as? T

    /** Gets a map with all the attribute keys and values on the request. */
    fun <T> attributeMap(): Map<String, T?> = servletRequest.attributeNames.asSequence().associate { it to attribute<T>(it) }

    /** Gets the request content length. */
    fun contentLength(): Int = servletRequest.contentLength

    /** Gets the request content type, or null. */
    fun contentType(): String? = servletRequest.contentType

    /** Gets a request cookie by name, or null. */
    fun cookie(name: String): String? = servletRequest.cookies?.find { name == it.name }?.value

    /** Gets a map with all the cookie keys and values on the request. */
    fun cookieMap(): Map<String, String> = servletRequest.cookies?.associate { it.name to it.value } ?: emptyMap()

    /** Gets a request header by name, or null. */
    fun header(header: String): String? = servletRequest.getHeader(header)

    /** Gets a map with all the header keys and values on the request. */
    fun headerMap(): Map<String, String> = servletRequest.headerNames.asSequence().associate { it to header(it)!! }

    /** Gets the request host, or null. */
    fun host(): String? = servletRequest.getHeader(Header.HOST)

    /** Gets the request ip. */
    fun ip(): String = servletRequest.remoteAddr

    /** Returns true if request is multipart. */
    fun isMultipart(): Boolean = header(Header.CONTENT_TYPE)?.toLowerCase()?.contains("multipart/") == true

    /** Returns true if request is multipart/form-data. */
    fun isMultipartFormData(): Boolean = header(Header.CONTENT_TYPE)?.toLowerCase()?.contains("multipart/form-data") == true

    /**
     * Gets the path that Javalin used to match the request.
     *
     * Ex: If the handler path is /users/:user-id,
     * and a browser GETs /users/123,
     * matchedPath() will return /users/:user-id
     */
    fun matchedPath() = matchedPath

    /**
     * Gets the path that Javalin used to match this request (excluding any AFTER handlers)
     */
    fun endpointHandlerPath() = if (handlerType != HandlerType.BEFORE) {
        endpointHandlerPath
    } else {
        throw IllegalStateException("Cannot access the endpoint handler path in a 'BEFORE' handler")
    }

    /** Gets the request method. */
    fun method(): String = servletRequest.method

    /** Gets the request path. */
    fun path(): String = servletRequest.requestURI

    /** Gets the request port. */
    fun port(): Int = servletRequest.serverPort

    /** Gets the request protocol. */
    fun protocol(): String = servletRequest.protocol

    /**
     * Gets a query param if it exists, else a default value (null if not specified explicitly).
     * Including a default value is mainly useful when calling from Java,
     * use elvis (queryParam(key) ?: default) instead in Kotlin.
     */
    @JvmOverloads
    fun queryParam(key: String, default: String? = null): String? = queryParams(key).firstOrNull() ?: default

    /**
     * Creates a [TypedValidator] for the queryParam() value, with the prefix "Query parameter '$key' with value '$value'"
     * Throws [BadRequestResponse] if validation fails.
     */
    @JvmOverloads
    fun <T> queryParam(key: String, clazz: Class<T>, default: String? = null) = Validator(queryParam(key, default), "Query parameter '$key' with value '${queryParam(key, default)}'").asClass(clazz)

    /** Reified version of [queryParam] (Kotlin only) */
    inline fun <reified T : Any> queryParam(key: String, default: String? = null) = queryParam(key, T::class.java, default)

    /** Gets a list of query params for the specified key, or empty list. */
    fun queryParams(key: String): List<String> = queryParamMap()[key] ?: emptyList()

    /** Gets a map with all the query param keys and values. */
    fun queryParamMap(): Map<String, List<String>> = ContextUtil.splitKeyValueStringAndGroupByKey(queryString() ?: "")

    /** Gets the request query string, or null. */
    fun queryString(): String? = servletRequest.queryString

    /** Gets the request scheme. */
    fun scheme(): String = servletRequest.scheme

    /** Sets an attribute for the user session. */
    fun sessionAttribute(attribute: String, value: Any?) = servletRequest.session.setAttribute(attribute, value)

    /** Gets specified attribute from the user session, or null. */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(attribute: String): T? = servletRequest.session.getAttribute(attribute) as? T

    /** Gets a map of all the attributes in the user session. */
    fun <T> sessionAttributeMap(): Map<String, T?> = servletRequest.session.attributeNames.asSequence().associate { it to sessionAttribute<T>(it) }

    /** Gets the request url. */
    fun url(): String = servletRequest.requestURL.toString()

    /** Gets the request context path. */
    fun contextPath(): String = servletRequest.contextPath

    /** Gets the request user agent, or null. */
    fun userAgent(): String? = servletRequest.getHeader(Header.USER_AGENT)

    ///////////////////////////////////////////////////////////////
    // Response-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the current response [Charset]. */
    private fun responseCharset() = try {
        Charset.forName(servletResponse.characterEncoding)
    } catch (e: Exception) {
        Charset.defaultCharset()
    }

    /**
     * Sets context result to the specified [String].
     * Will overwrite the current result if there is one.
     */
    fun result(resultString: String) = result(resultString.byteInputStream(responseCharset()))

    /** Gets the current context result as a [String] (if set). */
    fun resultString() = resultStream?.apply { reset() }?.readBytes()?.toString(responseCharset())

    /**
     * Sets context result to the specified [InputStream].
     * Will overwrite the current result if there is one.
     */
    fun result(resultStream: InputStream): Context {
        this.resultFuture = null
        this.resultStream = resultStream
        return this
    }

    /** Gets the current context result as an [InputStream] (if set). */
    fun resultStream(): InputStream? = resultStream

    /**
     * Sets context result to the specified CompletableFuture<String>
     * or CompletableFuture<InputStream>.
     * Will overwrite the current result if there is one.
     * Can only be called inside endpoint handlers (ones representing HTTP verbs).
     */
    fun result(future: CompletableFuture<*>): Context {
        resultStream = null
        if (handlerType.isHttpMethod() && !inExceptionHandler) {
            this.resultFuture = future
            return this
        }
        throw IllegalStateException("You can only set CompletableFuture results in endpoint handlers.")
    }

    /** Gets the current context result as a [CompletableFuture] (if set). */
    fun resultFuture(): CompletableFuture<*>? = resultFuture

    /** Sets response content type to specified [String] value. */
    fun contentType(contentType: String): Context {
        servletResponse.contentType = contentType
        return this
    }

    /** Sets response header by name and value. */
    fun header(headerName: String, headerValue: String): Context {
        servletResponse.setHeader(headerName, headerValue)
        return this
    }

    /** Sets the response status code and redirects to the specified location. */
    @JvmOverloads
    fun redirect(location: String, httpStatusCode: Int = HttpServletResponse.SC_MOVED_TEMPORARILY) {
        servletResponse.setHeader(Header.LOCATION, location)
        status(httpStatusCode)
        if (handlerType == HandlerType.BEFORE) {
            throw RedirectResponse(httpStatusCode)
        }
    }

    /** Sets the response status. */
    fun status(statusCode: Int): Context {
        servletResponse.status = statusCode
        return this
    }

    /** Gets the response status. */
    fun status(): Int = servletResponse.status

    /** Sets a cookie with name, value, and (overloaded) max-age. */
    @JvmOverloads
    fun cookie(name: String, value: String, maxAge: Int = -1): Context = cookie(Cookie(name, value).apply { setMaxAge(maxAge) })

    /** Sets a Cookie. */
    fun cookie(cookie: Cookie): Context {
        cookie.path = cookie.path ?: "/"
        servletResponse.addCookie(cookie)
        return this
    }

    /** Removes cookie specified by name and path (optional). */
    @JvmOverloads
    fun removeCookie(name: String, path: String? = null): Context {
        servletResponse.addCookie(Cookie(name, "").apply {
            this.path = path
            this.maxAge = 0
        })
        return this
    }

    /** Sets context result to specified html string and sets content-type to text/html. */
    fun html(html: String): Context = contentType("text/html").result(html)

    /**
     * Serializes object to a JSON-string using JavalinJson and sets it as the context result.
     * Sets content type to application/json.
     *
     * JavalinJson can be configured to use any mapping library.
     */
    fun json(obj: Any): Context {
        return contentType("application/json").result(JavalinJson.toJson(obj))
    }

    /**
     * Serializes the object resulting from the completion of the given future
     * to a JSON-string using JavalinJson and sets it as the context result.
     * Sets content type to application/json.
     *
     * JavalinJson can be configured to use any mapping library.
     */
    fun json(future: CompletableFuture<*>): Context {
        val mappingFuture = future.thenApply { JavalinJson.toJson(it) }
        return contentType("application/json").result(mappingFuture)
    }

    /**
     * Renders a file with specified values and sets it as the context result.
     * Also sets content-type to text/html.
     * Determines the correct rendering-function based on the file extension.
     */
    @JvmOverloads
    fun render(filePath: String, model: Map<String, Any?> = emptyMap()): Context {
        return html(JavalinRenderer.renderBasedOnExtension(filePath, model))
    }

}
