/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.config.contextResolver
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.CookieStore
import io.javalin.http.util.MultipartUtil
import io.javalin.http.util.SeekableWriter
import io.javalin.plugin.json.jsonMapper
import io.javalin.plugin.rendering.JavalinRenderer
import io.javalin.security.BasicAuthCredentials
import io.javalin.validation.BodyValidator
import io.javalin.validation.Validator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

/**
 * Provides access to functions for handling the request and response
 *
 * @see <a href="https://javalin.io/documentation#context">Context in docs</a>
 */
// don't suppress warnings, since annotated classes are ignored by dokka (yeah...)
interface Context {

    /** Servlet request */
    fun request(): HttpServletRequest
    /** Servlet response */
    fun response(): HttpServletResponse

    /** Gets an attribute from the Javalin instance serving the request */
    fun <T> appAttribute(key: String): T
    /** Gets the handler type of the current handler */
    fun handlerType(): HandlerType
    /** * Gets the path that was used to match request (also includes before/after paths) */
    fun matchedPath(): String
    /** Gets the endpoint path that was used to match request (null in before, available in endpoint/after) */
    fun endpointHandlerPath(): String

    ///////////////////////////////////////////////////////////////
    // Request-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the request content length. */
    fun contentLength(): Int = request().contentLength
    /** Gets the request content type, or null. */
    fun contentType(): String? = request().contentType
    /** Gets the request method. */
    fun method(): HandlerType
    /** Gets the request path. */
    fun path(): String = request().requestURI
    /** Gets the request port. */
    fun port(): Int = request().serverPort
    /** Gets the request protocol. */
    fun protocol(): String = request().protocol
    /** Gets the request context path. */
    fun contextPath(): String = request().contextPath
    /** Gets the request user agent, or null. */
    fun userAgent(): String? = request().getHeader(Header.USER_AGENT)
    /** Try to obtain request encoding from [Header.CONTENT_TYPE] header */
    fun characterEncoding(): String?

    /** Gets the request url. */
    fun url(): String = contextResolver().url.invoke(this)
    /** Gets the full request url, including query string (if present) */
    fun fullUrl(): String = contextResolver().fullUrl.invoke(this)
    /** Gets the request scheme. */
    fun scheme(): String = contextResolver().scheme.invoke(this)
    /** Gets the request host, or null. */
    fun host(): String? = contextResolver().host.invoke(this)
    /** Gets the request ip. */
    fun ip(): String = contextResolver().ip.invoke(this)

    /** Gets the request body as a [String]. */
    fun body(): String = bodyAsBytes().toString(Charset.forName(characterEncoding() ?: "UTF-8"))
    /**
     * Gets the request body as a [ByteArray].
     * Calling this method returns the body as a [ByteArray]. If [io.javalin.JavalinConfig.maxRequestSize]
     * is set and body is bigger than its value, a [io.javalin.http.HttpResponseException] is throw,
     * with status 413 CONTENT_TOO_LARGE.
     */
    fun bodyAsBytes(): ByteArray
    /** Maps a JSON body to a Java/Kotlin class using the registered [io.javalin.plugin.json.JsonMapper] */
    fun <T> bodyAsClass(clazz: Class<T>): T = jsonMapper().fromJsonString(body(), clazz)
    /** Maps a JSON body to a Java/Kotlin class using the registered [io.javalin.plugin.json.JsonMapper] */
    fun <T> bodyStreamAsClass(clazz: Class<T>): T = jsonMapper().fromJsonStream(request().inputStream, clazz)
    /** Gets the request body as a [InputStream] */
    fun bodyAsInputStream(): InputStream = request().inputStream
    /** Creates a typed [BodyValidator] for the body() value */
    fun <T> bodyValidator(clazz: Class<T>) = BodyValidator(body(), clazz, this.jsonMapper())

    /** Gets a form param if it exists, else null */
    fun formParam(key: String): String? = formParams(key).firstOrNull()
    /** Creates a typed [Validator] for the formParam() value */
    fun <T> formParamAsClass(key: String, clazz: Class<T>) = Validator.create(clazz, formParam(key), key)
    /** Gets a list of form params for the specified key, or empty list. */
    fun formParams(key: String): List<String> = formParamMap()[key] ?: emptyList()
    /** Gets a map with all the form param keys and values. */
    fun formParamMap(): Map<String, List<String>>

    /**
     * Gets a path param by name (ex: pathParam("param").
     *
     * Ex: If the handler path is /users/{user-id},
     * and a browser GETs /users/123,
     * pathParam("user-id") will return "123"
     */
    fun pathParam(key: String): String
    /** Creates a typed [Validator] for the pathParam() value */
    fun <T> pathParamAsClass(key: String, clazz: Class<T>) = Validator.create(clazz, pathParam(key), key)
    /** Gets a map of all the [pathParamAsClass] keys and values. */
    fun pathParamMap(): Map<String, String>

    /** Gets a query param if it exists, else null */
    fun queryParam(key: String): String? = queryParams(key).firstOrNull()
    /** Creates a typed [Validator] for the queryParam() value */
    fun <T> queryParamAsClass(key: String, clazz: Class<T>) = Validator.create(clazz, queryParam(key), key)
    /** Gets a list of query params for the specified key, or empty list. */
    fun queryParams(key: String): List<String> = queryParamMap()[key] ?: emptyList()
    /** Gets a map with all the query param keys and values. */
    fun queryParamMap(): Map<String, List<String>>
    /** Gets the request query string, or null. */
    fun queryString(): String? = request().queryString

    /** Sets an attribute for the user session. */
    fun sessionAttribute(key: String, value: Any?) = request().session.setAttribute(key, value)
    /** Gets specified attribute from the user session, or null. */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(key: String): T? = request().getSession(false)?.getAttribute(key) as? T
    /** */
    fun <T> consumeSessionAttribute(key: String) = sessionAttribute<T?>(key).also { this.sessionAttribute(key, null) }
    /** Sets an attribute for the user session, and caches it on the request */
    fun cachedSessionAttribute(key: String, value: Any?) = ContextUtil.cacheAndSetSessionAttribute(key, value, request())
    /** Gets specified attribute from the request attribute cache, or the user session, or null. */
    fun <T> cachedSessionAttribute(key: String): T? = ContextUtil.getCachedRequestAttributeOrSessionAttribute(key, request())
    /** Gets specified attribute from the request attribute cache, or the user session, or computes the value from callback. */
    fun <T> cachedSessionAttributeOrCompute(key: String, callback: (Context) -> T): T? = ContextUtil.cachedSessionAttributeOrCompute(callback, key, this)
    /** Gets a map of all the attributes in the user session. */
    fun sessionAttributeMap(): Map<String, Any?> = request().session.attributeNames.asSequence().associateWith { sessionAttribute(it) }

    /** Sets an attribute on the request(). Attributes are available to other handlers in the request lifecycle */
    fun attribute(key: String, value: Any?) = request().setAttribute(key, value)
    /** Gets the specified attribute from the request(). */
    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(key: String): T? = request().getAttribute(key) as? T
    /** Gets a map with all the attribute keys and values on the request(). */
    fun attributeMap(): Map<String, Any?> = request().attributeNames.asSequence().associateWith { attribute(it) as Any? }

    /** Gets cookie store used by this request */
    fun cookieStore(): CookieStore
    /** Gets a request cookie by name, or null. */
    fun cookie(name: String): String? = request().cookies?.find { name == it.name }?.value
    /** Gets a map with all the cookie keys and values on the request(). */
    fun cookieMap(): Map<String, String> = request().cookies?.associate { it.name to it.value } ?: emptyMap()

    /** Gets a request header by name, or null. */
    fun header(header: String): String? = request().getHeader(header)
    /** Creates a typed [Validator] for the header() value */
    fun <T> headerAsClass(header: String, clazz: Class<T>): Validator<T> = Validator.create(clazz, header(header), header)
    /** Gets a map with all the header keys and values on the request(). */
    fun headerMap(): Map<String, String> = request().headerNames.asSequence().associateWith { header(it)!! }

    /**
     * Checks whether basic-auth credentials from the request exists.
     *
     * Returns a Boolean which is true if there is an Authorization header with
     * Basic auth credentials. Returns false otherwise.
     */
    fun basicAuthCredentialsExist(): Boolean = ContextUtil.hasBasicAuthCredentials(header(Header.AUTHORIZATION))
    /**
     * Gets basic-auth credentials from the request, or throws.
     *
     * Returns a wrapper object [BasicAuthCredentials] which contains the
     * Base64 decoded username and password from the Authorization header.
     */
    fun basicAuthCredentials(): BasicAuthCredentials = ContextUtil.getBasicAuthCredentials(header(Header.AUTHORIZATION))

    /** Returns true if request is multipart. */
    fun isMultipart(): Boolean = header(Header.CONTENT_TYPE)?.lowercase(Locale.ROOT)?.contains("multipart/") == true
    /** Returns true if request is multipart/form-data. */
    fun isMultipartFormData(): Boolean = header(Header.CONTENT_TYPE)?.lowercase(Locale.ROOT)?.contains("multipart/form-data") == true

    /** Gets first [UploadedFile] for the specified name, or null. */
    fun uploadedFile(fileName: String): UploadedFile? = uploadedFiles(fileName).firstOrNull()
    /** Gets a list of [UploadedFile]s for the specified name, or empty list. */
    fun uploadedFiles(fileName: String): List<UploadedFile> = when {
        isMultipartFormData() -> MultipartUtil.getUploadedFiles(request(), fileName)
        else -> listOf()
    }
    /** Gets a list of [UploadedFile]s, or empty list. */
    fun uploadedFiles(): List<UploadedFile> = when {
        isMultipartFormData() -> MultipartUtil.getUploadedFiles(request())
        else -> listOf()
    }

    ///////////////////////////////////////////////////////////////
    // Response-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the current response [Charset]. */
    private fun responseCharset() = runCatching { Charset.forName(response().characterEncoding) }.getOrElse { Charset.defaultCharset() }

    /**
     * Writes the specified inputStream as a seekable stream.
     * This method is asynchronous and uses the global predefined executor
     * service stored in [appAttributes] as [ASYNC_EXECUTOR_KEY].
     * You can change this default in [io.javalin.config.JavalinConfig].
     *
     * @return the [CompletableFuture] used to write the seekable stream
     */
    fun writeSeekableStream(inputStream: InputStream, contentType: String, size: Long) = SeekableWriter.write(this, inputStream, contentType, size)
    /** */
    fun writeSeekableStream(inputStream: InputStream, contentType: String) = writeSeekableStream(inputStream, contentType, inputStream.available().toLong())

    /**
     * Sets context result to the specified [String].
     * Will overwrite the current result if there is one.
     */
    fun result(resultString: String) = result(resultString.byteInputStream(responseCharset()))
    /**
     * Sets context result to the specified array of bytes.
     * Will overwrite the current result if there is one.
     */
    fun result(resultBytes: ByteArray) = result(resultBytes.inputStream())
    /**
     * Sets context result to the specified [InputStream].
     * Will overwrite the current result if there is one.
     */
    fun result(resultStream: InputStream): Context {
        runCatching { resultStream()?.close() } // avoid memory leaks for multiple result() calls
        return this.future(CompletableFuture.completedFuture(resultStream), callback = { /* noop */ })
    }

    /** Gets the current [resultReference] as a [String] (if possible), and reset the underlying stream */
    fun resultString() = ContextUtil.readAndResetStreamIfPossible(resultStream(), responseCharset())
    /** */
    fun resultStream(): InputStream?

    /**
     * Utility function that allows to run async task on top of the [Context.future] method.
     * It means you should treat provided task as a result of this handler, and you can't use any other result function simultaneously.
     *
     * @param executor Thread-pool used to execute the given task,
     * by default this method will use global predefined executor service stored in [appAttributes] as [ASYNC_EXECUTOR_KEY].
     * You can change this default in [io.javalin.config.JavalinConfig].
     *
     * @param timeout Timeout in milliseconds,
     * by default it's 0 which means timeout watcher is disabled.
     *
     * @param onTimeout Timeout listener executed when [TimeoutException] is thrown in specified task.
     * This timeout listener is a part of request lifecycle, so you can still modify context here.
     *
     * @return As a result, function returns a new future that you can listen to.
     * The limitation is that you can't modify context after such event,
     * because it'll most likely be executed when the connection is already closed,
     * so it's just not thread-safe.
     */
    fun async(executor: ExecutorService, timeout: Long, onTimeout: (() -> Unit)?, task: Runnable): CompletableFuture<*>
    /** */
    fun async(timeout: Long = 0L, onTimeout: (() -> Unit)? = null, task: Runnable): CompletableFuture<*> = async(appAttribute(ASYNC_EXECUTOR_KEY), timeout, onTimeout, task)
    /** */
    fun async(task: Runnable): CompletableFuture<*> = async(task = task, timeout = 0L, onTimeout = null)

    /**
     * The main entrypoint for all async related functionalities exposed by [Context].
     *
     * @param future Future represents any delayed in time result.
     *  Upon this value Javalin will schedule further execution of this request().
     *  When servlet will detect that the given future is completed, request will be executed synchronously,
     *  otherwise request will be executed asynchronously by a thread which will complete the future.
     * @param launch Optional callback that provides a possibility to launch any kind of async execution in a thread-safe way.
     *  Any async task that will mutate [Context] should be submitted to the executor in this scope to eliminate race-conditions between threads.
     * @param callback Optional callback used to process result from the specified future.
     *  The default callback (used if no callback is provided) can be configured through [io.javalin.config.ContextResolver.defaultFutureCallback]
     * @throws IllegalStateException if result was already set
     */
    fun <T> future(future: CompletableFuture<T>, launch: Runnable?, callback: Consumer<T>?): Context
    /** See the main `future(CompletableFuture<T>, Runnable, Consumer<T>)` method for details. */
    fun <T> future(future: CompletableFuture<T>): Context = future(future = future, callback = null)
    /** See the main `future(CompletableFuture<T>, Runnable, Consumer<T>)` method for details. */
    fun <T> future(future: CompletableFuture<T>, callback: Consumer<T>?): Context = future(future = future, launch = null, callback = callback)
    /** Gets the current context result as a [CompletableFuture] (if set). */
    fun resultFuture(): CompletableFuture<*>?

    /** Sets response content type to specified [String] value. */
    fun contentType(contentType: String): Context = also { response().contentType = contentType }
    /** Sets response content type to specified [ContentType] value. */
    fun contentType(contentType: ContentType): Context = contentType(contentType.mimeType)

    /** Sets response header by name and value. */
    fun header(name: String, value: String): Context = also { response().setHeader(name, value) }

    /** Sets the response status code and redirects to the specified location. */
    fun redirect(location: String) = redirect(location = location, httpStatusCode = HttpServletResponse.SC_MOVED_TEMPORARILY)
    /** */
    fun redirect(location: String, httpStatusCode: Int)

    /** Sets the response status. */
    fun status(httpCode: HttpCode): Context = status(httpCode.status)
    /** Sets the response status. */
    fun status(statusCode: Int): Context = also { response().status = statusCode }
    /** Gets the response status. */
    fun status(): Int = response().status

    /** Sets a cookie with name, value, and (overloaded) max-age. */
    fun cookie(name: String, value: String): Context = cookie(name, value, -1)
    /** */
    fun cookie(name: String, value: String, maxAge: Int): Context = cookie(Cookie(name = name, value = value, maxAge = maxAge))
    /** Sets a Cookie. */
    fun cookie(cookie: Cookie): Context = also { response().setJavalinCookie(cookie) }

    /** Removes cookie specified by name and path (optional). */
    fun removeCookie(name: String, path: String?): Context
    /** */
    fun removeCookie(name: String): Context = removeCookie(name, "/")

    /**
     * Serializes object to a JSON-string using the registered [io.javalin.plugin.json.JsonMapper] and sets it as the context result.
     * Also sets content type to application/json.
     */
    fun json(obj: Any): Context = contentType(ContentType.APPLICATION_JSON).result(jsonMapper().toJsonString(obj))
    /**
     * Serializes object to a JSON-stream using the registered [io.javalin.plugin.json.JsonMapper] and sets it as the context result.
     * Also sets content type to application/json.
     */
    fun jsonStream(obj: Any): Context = contentType(ContentType.APPLICATION_JSON).result(jsonMapper().toJsonStream(obj))

    /** Sets context result to specified html string and sets content-type to text/html. */
    fun html(html: String): Context = contentType(ContentType.TEXT_HTML).result(html)

    /**
     * Renders a file with specified values and sets it as the context result.
     * Also sets content-type to text/html.
     * Determines the correct rendering-function based on the file extension.
     */
    fun render(filePath: String, model: Map<String, Any?>): Context = html(JavalinRenderer.renderBasedOnExtension(filePath, model, this))
    /** @see `render(String, Map<String, Any?>)` */
    fun render(filePath: String): Context = render(filePath, mutableMapOf())

}

/** Reified version of [bodyAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.bodyAsClass(): T = bodyAsClass(T::class.java)

/** Reified version of [formParamAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.formParamAsClass(key: String) = formParamAsClass(key, T::class.java)

/** Reified version of [bodyStreamAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.bodyStreamAsClass(): T = bodyStreamAsClass(T::class.java)

/** Reified version of [bodyValidator] (Kotlin only) */
inline fun <reified T : Any> Context.bodyValidator() = bodyValidator(T::class.java)

/** Reified version of [pathParamAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.pathParamAsClass(key: String) = pathParamAsClass(key, T::class.java)

/** Reified version of [headerAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.headerAsClass(header: String) = headerAsClass(header, T::class.java)

/** Reified version of [queryParamAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.queryParamAsClass(key: String) = queryParamAsClass(key, T::class.java)
