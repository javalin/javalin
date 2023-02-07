/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.config.contextResolver
import io.javalin.http.servlet.cacheAndSetSessionAttribute
import io.javalin.http.servlet.cachedSessionAttributeOrCompute
import io.javalin.http.servlet.attributeOrCompute
import io.javalin.http.servlet.getBasicAuthCredentials
import io.javalin.http.servlet.getCachedRequestAttributeOrSessionAttribute
import io.javalin.http.servlet.getRequestCharset
import io.javalin.http.servlet.readAndResetStreamIfPossible
import io.javalin.http.servlet.splitKeyValueStringAndGroupByKey
import io.javalin.http.servlet.throwContentTooLargeIfContentTooLarge
import io.javalin.http.util.AsyncUtil
import io.javalin.http.util.CookieStore
import io.javalin.http.util.MultipartUtil
import io.javalin.http.util.SeekableWriter
import io.javalin.json.jsonMapper
import io.javalin.rendering.JavalinRenderer
import io.javalin.rendering.fileRenderer
import io.javalin.security.BasicAuthCredentials
import io.javalin.util.function.ThrowingRunnable
import io.javalin.validation.BodyValidator
import io.javalin.validation.Validator
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeoutException
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

/**
 * Provides access to functions for handling the request and response
 *
 * @see <a href="https://javalin.io/documentation#context">Context in docs</a>
 */
// don't suppress warnings, since annotated classes are ignored by dokka (yeah...)
interface Context {

    /** Servlet request */
    fun req(): HttpServletRequest

    /** Servlet response */
    fun res(): HttpServletResponse

    /** Gets an attribute from the Javalin instance serving the request */
    fun <T> appAttribute(key: String): T

    /** Gets the handler type of the current handler */
    fun handlerType(): HandlerType

    /** Gets the path that was used to match request (also includes before/after paths) */
    fun matchedPath(): String

    /** Gets the endpoint path that was used to match request (null in before, available in endpoint/after) */
    fun endpointHandlerPath(): String

    ///////////////////////////////////////////////////////////////
    // Request-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the request content length. */
    fun contentLength(): Int = req().contentLength

    /** Gets the request content type, or null. */
    fun contentType(): String? = req().contentType

    /** Gets the request method. */
    fun method(): HandlerType = HandlerType.findByName(header(Header.X_HTTP_METHOD_OVERRIDE) ?: req().method)

    /** Gets the request path. */
    fun path(): String = req().requestURI

    /** Gets the request port. */
    fun port(): Int = req().serverPort

    /** Gets the request protocol. */
    fun protocol(): String = req().protocol

    /** Gets the request context path. */
    fun contextPath(): String = req().contextPath

    /** Gets the request user agent, or null. */
    fun userAgent(): String? = req().getHeader(Header.USER_AGENT)

    /** Try to obtain request encoding from [Header.CONTENT_TYPE] header */
    fun characterEncoding(): String? = getRequestCharset(this)

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
     * Calling this method returns the body as a [ByteArray]. If [io.javalin.config.JavalinConfig.maxRequestSize]
     * is set and body is bigger than its value, a [io.javalin.http.HttpResponseException] is throw,
     * with status 413 CONTENT_TOO_LARGE.
     */
    fun bodyAsBytes(): ByteArray {
        this.throwContentTooLargeIfContentTooLarge()
        return req().inputStream.readBytes()
    }

    /** Maps a JSON body to a Java/Kotlin class using the registered [io.javalin.json.JsonMapper] */
    fun <T> bodyAsClass(type: Type): T = jsonMapper().fromJsonString(body(), type)

    /** Maps a JSON body to a Java/Kotlin class using the registered [io.javalin.json.JsonMapper] */
    fun <T> bodyAsClass(clazz: Class<T>): T = bodyAsClass(type = clazz as Type)

    /** Maps a JSON body to a Java/Kotlin class using the registered [io.javalin.json.JsonMapper] */
    fun <T> bodyStreamAsClass(type: Type): T = jsonMapper().fromJsonStream(req().inputStream, type)

    /** Gets the underlying [InputStream] for the request body */
    fun bodyInputStream(): InputStream = req().inputStream

    /** Creates a typed [BodyValidator] for the body() value */
    fun <T> bodyValidator(clazz: Class<T>) = BodyValidator(body(), clazz, this.jsonMapper())

    /** Gets a form param if it exists, else null */
    fun formParam(key: String): String? = formParams(key).firstOrNull()

    /** Creates a typed [Validator] for the formParam() value */
    fun <T> formParamAsClass(key: String, clazz: Class<T>) = Validator.create(clazz, formParam(key), key)

    /** Gets a list of form params for the specified key, or empty list. */
    fun formParams(key: String): List<String> = formParamMap()[key] ?: emptyList()

    /** Gets a map with all the form param keys and values. */
    fun formParamMap(): Map<String, List<String>> = when {
        isMultipartFormData() -> MultipartUtil.getFieldMap(req())
        else -> splitKeyValueStringAndGroupByKey(body(), characterEncoding() ?: "UTF-8")
    }

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
    fun queryParamMap(): Map<String, List<String>> = splitKeyValueStringAndGroupByKey(queryString() ?: "", characterEncoding() ?: "UTF-8")

    /** Gets the request query string, or null. */
    fun queryString(): String? = req().queryString

    /** Sets an attribute for the user session. */
    fun sessionAttribute(key: String, value: Any?) = req().session.setAttribute(key, value)

    /** Gets specified attribute from the user session, or null. */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(key: String): T? = req().getSession(false)?.getAttribute(key) as? T

    /** Get session attribute, and set value to null */
    fun <T> consumeSessionAttribute(key: String) = sessionAttribute<T?>(key).also { this.sessionAttribute(key, null) }

    /** Sets an attribute for the user session, and caches it on the request */
    fun cachedSessionAttribute(key: String, value: Any?) = cacheAndSetSessionAttribute(key, value, req())

    /** Gets specified attribute from the request attribute cache, or the user session, or null. */
    fun <T> cachedSessionAttribute(key: String): T? = getCachedRequestAttributeOrSessionAttribute(key, req())

    /** Gets specified attribute from the request attribute cache, or the user session, or computes the value from callback. */
    fun <T> cachedSessionAttributeOrCompute(key: String, callback: (Context) -> T): T? = cachedSessionAttributeOrCompute(callback, key, this)

    /** Gets a map of all the attributes in the user session. */
    fun sessionAttributeMap(): Map<String, Any?> = req().session.attributeNames.asSequence().associateWith { sessionAttribute(it) }

    /** Sets an attribute on the request(). Attributes are available to other handlers in the request lifecycle */
    fun attribute(key: String, value: Any?) = req().setAttribute(key, value)

    /** Gets the specified attribute from the request(). */
    @Suppress("UNCHECKED_CAST")
    fun <T> attribute(key: String): T? = req().getAttribute(key) as? T

    /** Gets specified [attribute], or computes the value from callback (and sets the attribute) */
    fun <T> attributeOrCompute(key: String, callback: (Context) -> T): T? = attributeOrCompute(callback, key, this)

    /** Gets a map with all the attribute keys and values on the request(). */
    fun attributeMap(): Map<String, Any?> = req().attributeNames.asSequence().associateWith { attribute(it) as Any? }

    /** Gets cookie store used by this request */
    fun cookieStore(): CookieStore = CookieStore(this)

    /** Gets a request cookie by name, or null. */
    fun cookie(name: String): String? = req().getCookie(name)

    /** Gets a map with all the cookie keys and values on the request(). */
    fun cookieMap(): Map<String, String> = req().cookies?.associate { it.name to it.value } ?: emptyMap()

    /** Gets a request header by name, or null. */
    fun header(header: String): String? = req().getHeader(header)

    /** Creates a typed [Validator] for the header() value */
    fun <T> headerAsClass(header: String, clazz: Class<T>): Validator<T> = Validator.create(clazz, header(header), header)

    /** Gets a map with all the header keys and values on the request(). */
    fun headerMap(): Map<String, String> = req().headerNames.asSequence().associateWith { header(it)!! }

    /**
     * Gets basic-auth credentials from the request, or throws.
     *
     * Returns a wrapper object [BasicAuthCredentials] which contains the
     * Base64 decoded username and password from the Authorization header,
     * or null if basic-auth is not properly configured
     */
    fun basicAuthCredentials(): BasicAuthCredentials? = getBasicAuthCredentials(header(Header.AUTHORIZATION))

    /** Returns true if request is multipart. */
    fun isMultipart(): Boolean = header(Header.CONTENT_TYPE)?.lowercase(Locale.ROOT)?.contains("multipart/") == true

    /** Returns true if request is multipart/form-data. */
    fun isMultipartFormData(): Boolean = header(Header.CONTENT_TYPE)?.lowercase(Locale.ROOT)?.contains("multipart/form-data") == true

    /** Gets first [UploadedFile] for the specified name, or null. */
    fun uploadedFile(fileName: String): UploadedFile? = uploadedFiles(fileName).firstOrNull()

    /** Gets a list of [UploadedFile]s for the specified name, or empty list. */
    fun uploadedFiles(fileName: String): List<UploadedFile> = when {
        isMultipartFormData() -> MultipartUtil.getUploadedFiles(req(), fileName)
        else -> listOf()
    }

    /** Gets a list of [UploadedFile]s, or empty list. */
    fun uploadedFiles(): List<UploadedFile> = when {
        isMultipartFormData() -> MultipartUtil.getUploadedFiles(req())
        else -> listOf()
    }

    /**
     * Gets a map of all uploaded files as a map where the key is the name of the parameter the file was uploaded as and
     * the value is the list of files uploaded under that parameter.
     *
     * If called on a non-multipart request this returns an empty map
    */
    fun uploadedFileMap(): Map<String, List<UploadedFile>> = when {
        isMultipartFormData() -> MultipartUtil.getUploadedFileMap(req())
        else -> emptyMap()
    }

    ///////////////////////////////////////////////////////////////
    // Response-ish methods
    ///////////////////////////////////////////////////////////////

    /** Gets the current response [Charset]. */
    fun responseCharset(): Charset = runCatching { Charset.forName(res().characterEncoding) }.getOrElse { Charset.defaultCharset() }

    /**
     * Gets output-stream you can write to.
     * This stream by default uses compression specified in Javalin configuration,
     * if you're looking for raw, uncompressed servlet's output-stream, use `ctx.res().outputStream`.
     * @see [HttpServletResponse.getOutputStream]
     */
    fun outputStream(): ServletOutputStream

    /**
     * Writes the specified inputStream as a seekable stream.
     * You can change this default in [io.javalin.config.JavalinConfig].
     *
     * @return the [CompletableFuture] used to write the seekable stream
     */
    fun writeSeekableStream(inputStream: InputStream, contentType: String, totalBytes: Long) = SeekableWriter.write(this, inputStream, contentType, totalBytes)

    /**
     * Writes input stream to [writeSeekableStream] with currently available data ([InputStream.available])
     * @see writeSeekableStream]
     */
    fun writeSeekableStream(inputStream: InputStream, contentType: String) = writeSeekableStream(inputStream, contentType, inputStream.available().toLong())

    /**
     * Sets context result to the specified [String].
     * Will overwrite the current result if there is one.
     */
    fun result(resultString: String): Context = result(resultString.byteInputStream(responseCharset()))

    /**
     * Sets context result to the specified array of bytes.
     * Will overwrite the current result if there is one.
     */
    fun result(resultBytes: ByteArray): Context = result(resultBytes.inputStream())

    /**
     * Sets context result to the specified [InputStream].
     * Will overwrite the current result if there is one.
     */
    fun result(resultStream: InputStream): Context

    /** Gets the current [io.javalin.http.servlet.JavalinServletContext.resultInputStream] as a [String] (if set), and reset the underlying stream */
    fun result(): String? = readAndResetStreamIfPossible(resultInputStream(), responseCharset())

    /** Gets the underlying [InputStream] for the result ([io.javalin.http.servlet.JavalinServletContext.resultInputStream]) */
    fun resultInputStream(): InputStream?

    /**
     * Utility function that allows to run async task on top of the [Context.future] method.
     * It means you should treat provided task as a result of this handler, and you can't use any other result function simultaneously.
     *
     * @param executor Thread-pool used to execute the given task,
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
    fun async(executor: ExecutorService? = null, timeout: Long = 0L, onTimeout: Runnable?, task: ThrowingRunnable<Exception>) =
        AsyncUtil.submitAsyncTask(this, executor, timeout, onTimeout, task)

    /** @see [async] */
    fun async(timeout: Long, onTimeout: Runnable? = null, task: ThrowingRunnable<Exception>) = async(executor = null, timeout = timeout, onTimeout = onTimeout, task = task)

    /* @see [async] */
    fun async(task: ThrowingRunnable<Exception>) = async(onTimeout = null, task = task)

    /**
     * The main entrypoint for all async related functionalities exposed by [Context].
     *
     * @param future Future represents any delayed in time result.
     *  Upon this value Javalin will schedule further execution of this request().
     *  When servlet will detect that the given future is completed, request will be executed synchronously,
     *  otherwise request will be executed asynchronously by a thread which will complete the future.
     * @throws IllegalStateException if result was already set
     */
    fun future(future: Supplier<out CompletableFuture<*>>)

    /** Sets response content type to specified [String] value. */
    fun contentType(contentType: String): Context = also { res().contentType = contentType }

    /** Sets response content type to specified [ContentType] value. */
    fun contentType(contentType: ContentType): Context = contentType(contentType.mimeType)

    /** Sets response header by name and value. */
    fun header(name: String, value: String): Context = also { res().setHeader(name, value) }

    /** Redirects to location with given status. Skips HTTP handler if called in before-handler */
    fun redirect(location: String, status: HttpStatus)

    /** Redirects to location with status [HttpStatus.FOUND]. Skips HTTP handler if called in before-handler */
    fun redirect(location: String) = redirect(location, HttpStatus.FOUND)

    /** Sets the response status. */
    fun status(status: HttpStatus): Context = also { res().status = status.code }

    /** Sets the response status. */
    fun status(status: Int): Context = also { res().status = status }

    /** Gets the response status. For non-standard codes, [HttpStatus.UNKNOWN] is returned, the specific code can be obtained using [statusCode]  */
    fun status(): HttpStatus = HttpStatus.forStatus(res().status)

    /** Gets the response status code */
    fun statusCode(): Int = res().status

    /** Sets a cookie with name, value, and max-age = -1. */
    fun cookie(name: String, value: String): Context = cookie(name, value, -1)

    /** Sets a cookie with name, value and max-age property*/
    fun cookie(name: String, value: String, maxAge: Int): Context = cookie(Cookie(name = name, value = value, maxAge = maxAge))

    /** Sets a Cookie. */
    fun cookie(cookie: Cookie): Context = also { res().setJavalinCookie(cookie) }

    /** Removes cookie specified by name and path (optional). */
    fun removeCookie(name: String, path: String?): Context = also { res().removeCookie(name, path) }

    /** Removes cookie specified by name */
    fun removeCookie(name: String): Context = removeCookie(name, "/")

    /**
     * Serializes object to a JSON-string using the registered [io.javalin.json.JsonMapper] and sets it as the context result.
     * Also sets content type to application/json.
     */
    fun json(obj: Any, type: Type): Context = contentType(ContentType.APPLICATION_JSON).result(jsonMapper().toJsonString(obj, type))

    /** @see [json] */
    fun json(obj: Any): Context = json(obj, obj::class.java)

    /**
     * Serializes object to a JSON-stream using the registered [io.javalin.json.JsonMapper] and sets it as the context result.
     * Also sets content type to application/json.
     */
    fun jsonStream(obj: Any, type: Type): Context = contentType(ContentType.APPLICATION_JSON).result(jsonMapper().toJsonStream(obj, type))
    /** @see [jsonStream] */
    fun jsonStream(obj: Any): Context = jsonStream(obj, obj::class.java)

    /**
     * Consumes the specified stream with the configured JsonMapper, which transforms the stream's
     * content to JSON, writing the results directly to the response's `outputStream` as the stream
     * is consumed. This function call is synchronous, and may be wrapped in `ctx.async { }` if needed.
     */
    fun writeJsonStream(stream: Stream<*>) = jsonMapper().writeToOutputStream(
        stream, this.contentType(ContentType.APPLICATION_JSON).outputStream()
    )

    /** Sets context result to specified html string and sets content-type to text/html. */
    fun html(html: String): Context = contentType(ContentType.TEXT_HTML).result(html)

    /**
     * Renders a file with specified values and sets it as the context result.
     * Also sets content-type to text/html.
     * Determines the correct rendering-function based on the file extension.
     */
    fun render(filePath: String, model: Map<String, Any?>): Context = html(fileRenderer().render(filePath, model, this))

    /** @see render() */
    fun render(filePath: String): Context = render(filePath, mutableMapOf())

}

/** Reified version of [Context.json] (Kotlin only) */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> Context.jsonAsType(obj: T): Context = json(obj, typeOf<T>().javaType)

/** Reified version of [Context.bodyAsClass] (Kotlin only) */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> Context.bodyAsClass(): T = bodyAsClass(typeOf<T>().javaType)

/** Reified version of [Context.bodyStreamAsClass] (Kotlin only) */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> Context.bodyStreamAsClass(): T = bodyStreamAsClass(typeOf<T>().javaType)

/** Reified version of [Context.bodyValidator] (Kotlin only) */
inline fun <reified T : Any> Context.bodyValidator(): BodyValidator<T> = bodyValidator(T::class.java)

/** Reified version of [Context.pathParamAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.pathParamAsClass(key: String): Validator<T> = pathParamAsClass(key, T::class.java)

/** Reified version of [Context.headerAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.headerAsClass(header: String): Validator<T> = headerAsClass(header, T::class.java)

/** Reified version of [Context.queryParamAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.queryParamAsClass(key: String): Validator<T> = queryParamAsClass(key, T::class.java)

/** Reified version of [Context.formParamAsClass] (Kotlin only) */
inline fun <reified T : Any> Context.formParamAsClass(key: String): Validator<T> = formParamAsClass(key, T::class.java)
