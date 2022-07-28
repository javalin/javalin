/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.config.contextResolver
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.MultipartUtil
import io.javalin.http.util.SeekableWriter
import io.javalin.plugin.json.jsonMapper
import io.javalin.plugin.rendering.JavalinRenderer
import io.javalin.security.BasicAuthCredentials
import io.javalin.util.exceptionallyAccept
import io.javalin.util.isCompletedSuccessfully
import io.javalin.validation.BodyValidator
import io.javalin.validation.Validator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

/** Defines default [ExecutorService] used by [Context.future] */
const val ASYNC_EXECUTOR_KEY = "javalin-context-async-executor"

/**
 * Provides access to functions for handling the request and response
 *
 * @see <a href="https://javalin.io/documentation#context">Context in docs</a>
 */
// don't suppress warnings, since annotated classes are ignored by dokka (yeah...)
open class Context(
    req: HttpServletRequest,
    res: HttpServletResponse,
    appAttributes: Map<String, Any> = mapOf(),
) {

    val state: ContextState = ContextState(req, res, appAttributes)

    fun req() = state.req
    fun res() = state.res

    /** Gets the handler type of the current handler */
    fun handlerType(): HandlerType = state.handlerType

    /** Gets an attribute from the Javalin instance serving the request */
    fun <T> appAttribute(key: String): T = state.appAttributes[key] as T

    fun cookieStore() = state.cookieStore

    /**
     * Gets the path that was used to match request (also includes before/after paths)
     */
    fun matchedPath() = state.matchedPath

    /**
     * Gets the endpoint path that was used to match request (null in before, available in endpoint/after)
     */
    fun endpointHandlerPath() = if (handlerType() != HandlerType.BEFORE) {
        state.endpointHandlerPath
    } else {
        throw IllegalStateException("Cannot access the endpoint handler path in a 'BEFORE' handler")
    }

    ///////////////////////////////////////////////////////////////
    // Request-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the request body as a [String]. */
    fun body(): String = bodyAsBytes().toString(Charset.forName(state.characterEncoding))

    /** Gets the request body as a [ByteArray].
     * Calling this method returns the body as a [ByteArray]. If [io.javalin.JavalinConfig.maxRequestSize]
     * is set and body is bigger than its value, a [io.javalin.http.HttpResponseException] is throw,
     * with status 413 CONTENT_TOO_LARGE.
     */
    fun bodyAsBytes(): ByteArray = state.body

    /** Maps a JSON body to a Java/Kotlin class using the registered [io.javalin.plugin.json.JsonMapper] */
    fun <T> bodyAsClass(clazz: Class<T>): T = jsonMapper().fromJsonString(body(), clazz)

    /** Reified version of [bodyAsClass] (Kotlin only) */
    inline fun <reified T : Any> bodyAsClass(): T = bodyAsClass(T::class.java)

    /** Maps a JSON body to a Java/Kotlin class using the registered [io.javalin.plugin.json.JsonMapper] */
    fun <T> bodyStreamAsClass(clazz: Class<T>): T = jsonMapper().fromJsonStream(req().inputStream, clazz)

    /** Reified version of [bodyStreamAsClass] (Kotlin only) */
    inline fun <reified T : Any> bodyStreamAsClass(): T = bodyStreamAsClass(T::class.java)

    /** Gets the request body as a [InputStream] */
    fun bodyAsInputStream(): InputStream = req().inputStream

    /** Creates a typed [BodyValidator] for the body() value */
    fun <T> bodyValidator(clazz: Class<T>) = BodyValidator(body(), clazz, this.jsonMapper())

    /** Reified version of [bodyValidator] (Kotlin only) */
    inline fun <reified T : Any> bodyValidator() = bodyValidator(T::class.java)

    /** Gets first [UploadedFile] for the specified name, or null. */
    fun uploadedFile(fileName: String): UploadedFile? = uploadedFiles(fileName).firstOrNull()

    /** Gets a list of [UploadedFile]s for the specified name, or empty list. */
    fun uploadedFiles(fileName: String): List<UploadedFile> {
        return if (isMultipartFormData()) MultipartUtil.getUploadedFiles(req(), fileName) else listOf()
    }

    /** Gets a list of [UploadedFile]s, or empty list. */
    fun uploadedFiles(): List<UploadedFile> {
        return if (isMultipartFormData()) MultipartUtil.getUploadedFiles(req()) else listOf()
    }

    /** Gets a form param if it exists, else null */
    fun formParam(key: String): String? = formParams(key).firstOrNull()

    /** Creates a typed [Validator] for the formParam() value */
    fun <T> formParamAsClass(key: String, clazz: Class<T>) = Validator.create(clazz, formParam(key), key)

    /** Reified version of [formParamAsClass] (Kotlin only) */
    inline fun <reified T : Any> formParamAsClass(key: String) = formParamAsClass(key, T::class.java)

    /** Gets a list of form params for the specified key, or empty list. */
    fun formParams(key: String): List<String> = formParamMap()[key] ?: emptyList()

    /** Gets a map with all the form param keys and values. */
    fun formParamMap(): Map<String, List<String>> = state.formParams

    /**
     * Gets a path param by name (ex: pathParam("param").
     *
     * Ex: If the handler path is /users/{user-id},
     * and a browser GETs /users/123,
     * pathParam("user-id") will return "123"
     */
    fun pathParam(key: String): String = ContextUtil.pathParamOrThrow(state.pathParamMap, key, state.matchedPath)

    /** Creates a typed [Validator] for the pathParam() value */
    fun <T> pathParamAsClass(key: String, clazz: Class<T>) = Validator.create(clazz, pathParam(key), key)

    /** Reified version of [pathParamAsClass] (Kotlin only) */
    inline fun <reified T : Any> pathParamAsClass(key: String) = pathParamAsClass(key, T::class.java)

    /** Gets a map of all the [pathParamAsClass] keys and values. */
    fun pathParamMap(): Map<String, String> = Collections.unmodifiableMap(state.pathParamMap)

    /**
     * Checks whether or not basic-auth credentials from the request exists.
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

    /** Sets an attribute on the request. Attributes are available to other handlers in the request lifecycle */
    fun attribute(key: String, value: Any?) = req().setAttribute(key, value)

    /** Gets the specified attribute from the request. */
    fun <T> attribute(key: String): T? = req().getAttribute(key) as? T

    /** Gets a map with all the attribute keys and values on the request. */
    fun attributeMap(): Map<String, Any?> = req().attributeNames.asSequence().associateWith { attribute(it) as Any? }

    /** Gets the request content length. */
    fun contentLength(): Int = req().contentLength

    /** Gets the request content type, or null. */
    fun contentType(): String? = req().contentType

    /** Gets a request cookie by name, or null. */
    fun cookie(name: String): String? = req().getCookie(name)

    /** Gets a map with all the cookie keys and values on the request. */
    fun cookieMap(): Map<String, String> = req().cookies?.associate { it.name to it.value } ?: emptyMap()

    /** Gets a request header by name, or null. */
    fun header(header: String): String? = req().getHeader(header)

    /** Creates a typed [Validator] for the header() value */
    fun <T> headerAsClass(header: String, clazz: Class<T>): Validator<T> = Validator.create(clazz, header(header), header)

    /** Reified version of [headerAsClass] (Kotlin only) */
    inline fun <reified T : Any> headerAsClass(header: String) = headerAsClass(header, T::class.java)

    /** Gets a map with all the header keys and values on the request. */
    fun headerMap(): Map<String, String> = req().headerNames.asSequence().associateWith { header(it)!! }

    /** Gets the request host, or null. */
    fun host(): String? = contextResolver().host.invoke(this)

    /** Gets the request ip. */
    fun ip(): String = contextResolver().ip.invoke(this)

    /** Returns true if request is multipart. */
    fun isMultipart(): Boolean = MultipartUtil.isMultipart(header(Header.CONTENT_TYPE))

    /** Returns true if request is multipart/form-data. */
    fun isMultipartFormData(): Boolean = MultipartUtil.isMultipartFormData(header(Header.CONTENT_TYPE))

    /** Gets the request method. */
    fun method(): HandlerType = state.method

    /** Gets the request path. */
    fun path(): String = req().requestURI

    /** Gets the request port. */
    fun port(): Int = req().serverPort

    /** Gets the request protocol. */
    fun protocol(): String = req().protocol

    /** Gets a query param if it exists, else null */
    fun queryParam(key: String): String? = queryParams(key).firstOrNull()

    /** Creates a typed [Validator] for the queryParam() value */
    fun <T> queryParamAsClass(key: String, clazz: Class<T>) = Validator.create(clazz, queryParam(key), key)

    /** Reified version of [queryParamAsClass] (Kotlin only) */
    inline fun <reified T : Any> queryParamAsClass(key: String) = queryParamAsClass(key, T::class.java)

    /** Gets a list of query params for the specified key, or empty list. */
    fun queryParams(key: String): List<String> = queryParamMap()[key] ?: emptyList()

    /** Gets a map with all the query param keys and values. */
    fun queryParamMap(): Map<String, List<String>> = state.queryParams

    /** Gets the request query string, or null. */
    fun queryString(): String? = req().queryString

    /** Gets the request scheme. */
    fun scheme(): String = contextResolver().scheme.invoke(this)

    /** Sets an attribute for the user session. */
    fun sessionAttribute(key: String, value: Any?) = req().session.setAttribute(key, value)

    /** Gets specified attribute from the user session, or null. */
    fun <T> sessionAttribute(key: String): T? = req().getSession(false)?.getAttribute(key) as? T

    fun <T> consumeSessionAttribute(key: String) = sessionAttribute<T?>(key).also { this.sessionAttribute(key, null) }

    /** Sets an attribute for the user session, and caches it on the request */
    fun cachedSessionAttribute(key: String, value: Any?) = ContextUtil.cacheAndSetSessionAttribute(key, value, req())

    /** Gets specified attribute from the request attribute cache, or the user session, or null. */
    fun <T> cachedSessionAttribute(key: String): T? = ContextUtil.getCachedRequestAttributeOrSessionAttribute(key, req())

    /** Gets specified attribute from the request attribute cache, or the user session, or computes the value from callback. */
    fun <T> cachedSessionAttributeOrCompute(key: String, callback: (Context) -> T): T? = ContextUtil.cachedSessionAttributeOrCompute(callback, key, this)

    /** Gets a map of all the attributes in the user session. */
    fun sessionAttributeMap(): Map<String, Any?> = req().session.attributeNames.asSequence().associateWith { sessionAttribute(it) }

    /** Gets the request url. */
    fun url(): String = contextResolver().url.invoke(this)

    /** Gets the full request url, including query string (if present) */
    fun fullUrl(): String = contextResolver().fullUrl.invoke(this)

    /** Gets the request context path. */
    fun contextPath(): String = req().contextPath

    /** Gets the request user agent, or null. */
    fun userAgent(): String? = req().getHeader(Header.USER_AGENT)

    ///////////////////////////////////////////////////////////////
    // Response-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the current response [Charset]. */
    private fun responseCharset() = try {
        Charset.forName(res().characterEncoding)
    } catch (e: Exception) {
        Charset.defaultCharset()
    }

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

    /** Gets the current [resultReference] as a [String] (if possible), and reset the underlying stream */
    fun resultString() = ContextUtil.readAndResetStreamIfPossible(resultStream(), responseCharset())

    /**
     * Sets context result to the specified [InputStream].
     * Will overwrite the current result if there is one.
     */
    fun result(resultStream: InputStream): Context {
        runCatching { resultStream()?.close() } // avoid memory leaks for multiple result() calls
        return this.future(CompletableFuture.completedFuture(resultStream), callback = { /* noop */ })
    }

    /**
     * Writes the specified inputStream as a seekable stream.
     * This method is asynchronous and uses the global predefined executor
     * service stored in [ContextState.appAttributes] as [ASYNC_EXECUTOR_KEY].
     * You can change this default in [io.javalin.config.JavalinConfig].
     *
     * @return the [CompletableFuture] used to write the seekable stream
     */
    @JvmOverloads
    fun writeSeekableStream(inputStream: InputStream, contentType: String, size: Long = inputStream.available().toLong()) =
        SeekableWriter.write(this, inputStream, contentType, size)

    fun resultStream(): InputStream? = state.resultReference.get().let { result ->
        result.future
            ?.takeIf { it.isCompletedSuccessfully() }
            ?.get() as? InputStream?
            ?: result.previous
    }

    /**
     * Utility function that allows to run async task on top of the [Context.future] method.
     * It means you should treat provided task as a result of this handler, and you can't use any other result function simultaneously.
     *
     * @param executor Thread-pool used to execute the given task,
     * by default this method will use global predefined executor service stored in [ContextState.appAttributes] as [ASYNC_EXECUTOR_KEY].
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
    @JvmOverloads
    fun async(
        executor: ExecutorService = appAttribute(ASYNC_EXECUTOR_KEY),
        timeout: Long = 0L,
        onTimeout: (() -> Unit)? = null,
        task: Runnable
    ): CompletableFuture<*> {
        val await = CompletableFuture<Any?>()

        future(
            future = await,
            launch = {
                CompletableFuture.runAsync(task, executor)
                    .thenAccept { await.complete(null) }
                    .let { if (timeout > 0) it.orTimeout(timeout, MILLISECONDS) else it }
                    .exceptionallyAccept {
                        when {
                            onTimeout != null && it is TimeoutException -> onTimeout.invoke().run { await.complete(null) }
                            else -> await.completeExceptionally(it) // catch standard exception
                        }
                    }
                    .exceptionallyAccept { await.completeExceptionally(it) } // catch exception from timeout listener
            },
            callback = { /* noop */ }
        )

        return await
    }

    /**
     * See the main `future(CompletableFuture<T>, Runnable, Consumer<T>)` method for details.
     */
    @JvmOverloads
    fun <T> future(future: CompletableFuture<T>, callback: Consumer<T>? = null): Context =
        future(future = future, launch = null, callback = callback)

    /**
     * The main entrypoint for all async related functionalities exposed by [Context].
     *
     * @param future Future represents any delayed in time result.
     *  Upon this value Javalin will schedule further execution of this request.
     *  When servlet will detect that the given future is completed, request will be executed synchronously,
     *  otherwise request will be executed asynchronously by a thread which will complete the future.
     * @param launch Optional callback that provides a possibility to launch any kind of async execution in a thread-safe way.
     *  Any async task that will mutate [Context] should be submitted to the executor in this scope to eliminate race-conditions between threads.
     * @param callback Optional callback used to process result from the specified future.
     *  The default callback (used if no callback is provided) can be configured through [io.javalin.config.ContextResolver.defaultFutureCallback]
     * @throws IllegalStateException if result was already set
     */
    fun <T> future(future: CompletableFuture<T>, launch: Runnable?, callback: Consumer<T>? = null): Context = also {
        if (state.resultReference.get().future != null) {
            throw IllegalStateException("Cannot override result")
        }
        state.resultReference.updateAndGet { oldResult ->
            oldResult.future?.cancel(true)
            Result(
                previous = oldResult.previous,
                future = future,
                launch = launch,
                callback = callback
            )
        }
    }

    /** Gets the current context result as a [CompletableFuture] (if set). */
    fun resultFuture(): CompletableFuture<*>? = state.resultReference.get().future

    /** Sets response content type to specified [String] value. */
    fun contentType(contentType: String): Context {
        res().contentType = contentType
        return this
    }

    /** Sets response content type to specified [ContentType] value. */
    fun contentType(contentType: ContentType): Context =
        contentType(contentType.mimeType)

    /** Sets response header by name and value. */
    fun header(name: String, value: String): Context {
        res().setHeader(name, value)
        return this
    }

    /** Sets the response status code and redirects to the specified location. */
    @JvmOverloads
    fun redirect(location: String, httpStatusCode: Int = HttpServletResponse.SC_MOVED_TEMPORARILY) {
        res().setHeader(Header.LOCATION, location)
        status(httpStatusCode)
        if (handlerType() == HandlerType.BEFORE) {
            throw RedirectResponse(httpStatusCode)
        }
    }

    /** Sets the response status. */
    fun status(httpCode: HttpCode): Context =
        status(httpCode.status)

    /** Sets the response status. */
    fun status(statusCode: Int): Context {
        res().status = statusCode
        return this
    }

    /** Gets the response status. */
    fun status(): Int = res().status

    /** Sets a cookie with name, value, and (overloaded) max-age. */
    @JvmOverloads
    fun cookie(name: String, value: String, maxAge: Int = -1) = cookie(Cookie(name = name, value = value, maxAge = maxAge))

    /** Sets a Cookie. */
    fun cookie(cookie: Cookie): Context {
        res().setJavalinCookie(cookie)
        return this
    }

    /** Removes cookie specified by name and path (optional). */
    @JvmOverloads
    fun removeCookie(name: String, path: String = "/"): Context {
        res().removeCookie(name, path)
        return this
    }

    /** Sets context result to specified html string and sets content-type to text/html. */
    fun html(html: String): Context = contentType(ContentType.TEXT_HTML).result(html)

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

    /**
     * Renders a file with specified values and sets it as the context result.
     * Also sets content-type to text/html.
     * Determines the correct rendering-function based on the file extension.
     */
    @JvmOverloads
    fun render(filePath: String, model: Map<String, Any?> = mutableMapOf()): Context {
        return html(JavalinRenderer.renderBasedOnExtension(filePath, model, this))
    }

}
