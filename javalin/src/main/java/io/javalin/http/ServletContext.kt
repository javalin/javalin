/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.http.util.ContextUtil
import io.javalin.http.util.ContextUtil.throwContentTooLargeIfContentTooLarge
import io.javalin.http.util.CookieStore
import io.javalin.http.util.MultipartUtil
import io.javalin.http.util.SeekableWriter
import io.javalin.util.exceptionallyAccept
import io.javalin.util.isCompletedSuccessfully
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import jakarta.servlet.http.Cookie as JakartaCookie

/** Defines default [ExecutorService] used by [Context.future] */
const val ASYNC_EXECUTOR_KEY = "javalin-context-async-executor"

open class ServletContext(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
    @JvmSynthetic internal val appAttributes: Map<String, Any> = mapOf()
) : Context {

    internal var matchedPath = ""
    internal var endpointHandlerPath = ""
    internal var pathParamMap = mapOf<String, String>()
    internal var handlerType = HandlerType.BEFORE
    internal val resultReference = AtomicReference<Result<out Any?>>(Result())

    override fun request(): HttpServletRequest = request
    override fun response(): HttpServletResponse = response

    private val characterEncoding by lazy { ContextUtil.getRequestCharset(this) ?: "UTF-8" }
    override fun characterEncoding(): String? = characterEncoding

    private val cookieStore by lazy { CookieStore(this) }
    override fun cookieStore() = cookieStore

    @Suppress("UNCHECKED_CAST")
    override fun <T> appAttribute(key: String): T = appAttributes[key] as T

    private val method by lazy { HandlerType.findByName(header(Header.X_HTTP_METHOD_OVERRIDE) ?: request().method) }
    override fun method(): HandlerType = method

    override fun handlerType(): HandlerType = handlerType
    override fun matchedPath(): String = matchedPath

    override fun endpointHandlerPath() = when {
        handlerType != HandlerType.BEFORE -> endpointHandlerPath
        else -> throw IllegalStateException("Cannot access the endpoint handler path in a 'BEFORE' handler")
    }

    private val body by lazy {
        this.throwContentTooLargeIfContentTooLarge()
        request.inputStream.readBytes()
    }
    override fun bodyAsBytes(): ByteArray = body

    /** using an additional map lazily so no new objects are created whenever ctx.formParam*() is called */
    private val formParams by lazy {
        if (isMultipartFormData()) MultipartUtil.getFieldMap(request)
        else ContextUtil.splitKeyValueStringAndGroupByKey(body(), characterEncoding)
    }

    override fun formParamMap(): Map<String, List<String>> = formParams
    override fun pathParamMap(): Map<String, String> =Collections.unmodifiableMap(pathParamMap)
    override fun pathParam(key: String): String = ContextUtil.pathParamOrThrow(pathParamMap, key, matchedPath)

    /** using an additional map lazily so no new objects are created whenever ctx.formParam*() is called */
    private val queryParams by lazy { ContextUtil.splitKeyValueStringAndGroupByKey(queryString() ?: "", characterEncoding) }
    override fun queryParamMap(): Map<String, List<String>> = queryParams

    override fun redirect(location: String, httpStatusCode: Int) {
        header(Header.LOCATION, location).status(httpStatusCode)
        if (handlerType() == HandlerType.BEFORE) {
            throw RedirectResponse(httpStatusCode)
        }
    }

    override fun removeCookie(name: String, path: String?): Context = also {
        response().addCookie(JakartaCookie(name, "").apply {
            this.path = path
            this.maxAge = 0
        })
    }

    override fun result(resultString: String) = result(resultString.byteInputStream(responseCharset()))
    override fun result(resultBytes: ByteArray) = result(resultBytes.inputStream())

    override fun result(resultStream: InputStream): Context {
        runCatching { resultStream()?.close() } // avoid memory leaks for multiple result() calls
        return this.future(CompletableFuture.completedFuture(resultStream), callback = { /* noop */ })
    }
    override fun writeSeekableStream(inputStream: InputStream, contentType: String, size: Long) =
        SeekableWriter.write(this, inputStream, contentType, size)

    override fun async(executor: ExecutorService, timeout: Long, onTimeout: (() -> Unit)?, task: Runnable): CompletableFuture<*> {
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

    override fun <T> future(future: CompletableFuture<T>, launch: Runnable?, callback: Consumer<T>?): Context = also {
        if (resultReference.get().future != null) {
            throw IllegalStateException("Cannot override result")
        }
        resultReference.updateAndGet { oldResult ->
            oldResult.future?.cancel(true)
            Result(
                previous = oldResult.previous,
                future = future,
                launch = launch,
                callback = callback
            )
        }
    }

    override fun resultStream(): InputStream? = resultReference.get().let { result ->
        result.future
            ?.takeIf { it.isCompletedSuccessfully() }
            ?.get() as? InputStream?
            ?: result.previous
    }

    override fun resultFuture(): CompletableFuture<*>? = resultReference.get().future
    override fun resultString(): String? = ContextUtil.readAndResetStreamIfPossible(resultStream(), responseCharset())

    private fun responseCharset(): Charset =
        runCatching { Charset.forName(response.characterEncoding) }
            .getOrNull()
            ?: Charset.defaultCharset()

}
