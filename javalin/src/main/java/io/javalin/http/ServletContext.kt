/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.http.util.AsyncUtil
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.ContextUtil.throwContentTooLargeIfContentTooLarge
import io.javalin.http.util.CookieStore
import io.javalin.http.util.MultipartUtil
import io.javalin.http.util.SeekableWriter
import io.javalin.util.isCompletedSuccessfully
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import jakarta.servlet.http.Cookie as JakartaCookie

/** Defines default [ExecutorService] used by [Context.future] */
const val ASYNC_EXECUTOR_KEY = "javalin-context-async-executor"

open class ServletContext(
    override val req: HttpServletRequest,
    override val res: HttpServletResponse,
    internal val appAttributes: Map<String, Any> = mapOf(),
    internal val resultReference: AtomicReference<Result<out Any?>> = AtomicReference(Result()),
    internal var pathParamMap: Map<String, String> = mapOf(),
    internal var matchedPath: String = "",
    internal var endpointHandlerPath: String = "",
    internal var handlerType: HandlerType = HandlerType.BEFORE,
) : Context {

    private val characterEncoding by lazy { ContextUtil.getRequestCharset(this) ?: "UTF-8" }
    override fun characterEncoding(): String? = characterEncoding

    private val cookieStore by lazy { CookieStore(this) }
    override fun cookieStore() = cookieStore

    @Suppress("UNCHECKED_CAST")
    override fun <T> appAttribute(key: String): T = appAttributes[key] as T

    private val method by lazy { HandlerType.findByName(header(Header.X_HTTP_METHOD_OVERRIDE) ?: req.method) }
    override fun method(): HandlerType = method

    override fun handlerType(): HandlerType = handlerType
    override fun matchedPath(): String = matchedPath

    override fun endpointHandlerPath() = when {
        handlerType != HandlerType.BEFORE -> endpointHandlerPath
        else -> throw IllegalStateException("Cannot access the endpoint handler path in a 'BEFORE' handler")
    }

    private val body by lazy {
        this.throwContentTooLargeIfContentTooLarge()
        req.inputStream.readBytes()
    }
    override fun bodyAsBytes(): ByteArray = body

    /** using an additional map lazily so no new objects are created whenever ctx.formParam*() is called */
    private val formParams by lazy {
        if (isMultipartFormData()) MultipartUtil.getFieldMap(req)
        else ContextUtil.splitKeyValueStringAndGroupByKey(body(), characterEncoding)
    }
    override fun formParamMap(): Map<String, List<String>> = formParams

    override fun pathParamMap(): Map<String, String> = Collections.unmodifiableMap(pathParamMap)
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
        res.addCookie(JakartaCookie(name, "").apply {
            this.path = path
            this.maxAge = 0
        })
    }

    override fun resultStream(): InputStream? = resultReference.get().let { result ->
        result.future
            ?.takeIf { it.isCompletedSuccessfully() }
            ?.get() as? InputStream?
            ?: result.previous
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

    override fun resultFuture(): CompletableFuture<*>? = resultReference.get().future

}
