/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.http.HandlerType.AFTER
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.ContextUtil.throwContentTooLargeIfContentTooLarge
import io.javalin.http.util.CookieStore
import io.javalin.http.util.MultipartUtil
import io.javalin.routing.HandlerEntry
import io.javalin.util.isCompletedSuccessfully
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class DefaultContext(
    private var req: HttpServletRequest,
    private val res: HttpServletResponse,
    private val appAttributes: Map<String, Any> = mapOf(),
    private var handlerType: HandlerType = HandlerType.BEFORE,
    private var matchedPath: String = "",
    private var pathParamMap: Map<String, String> = mapOf(),
    internal var endpointHandlerPath: String = "",
    internal val resultReference: AtomicReference<Result<out Any?>> = AtomicReference(Result()),
) : Context {

    fun changeBaseRequest(req: HttpServletRequest) = also {
        this.req = req
    }

    fun update(handlerEntry: HandlerEntry, requestUri: String) = also {
        matchedPath = handlerEntry.path
        pathParamMap = handlerEntry.extractPathParams(requestUri)
        handlerType = handlerEntry.type
        if (handlerType != AFTER) {
            endpointHandlerPath = handlerEntry.path
        }
    }

    override fun req(): HttpServletRequest = req
    override fun res(): HttpServletResponse = res

    @Suppress("UNCHECKED_CAST")
    override fun <T> appAttribute(key: String): T = appAttributes[key] as T

    override fun endpointHandlerPath() = when {
        handlerType() != HandlerType.BEFORE -> endpointHandlerPath
        else -> throw IllegalStateException("Cannot access the endpoint handler path in a 'BEFORE' handler")
    }

    private val characterEncoding by lazy { super.characterEncoding() }
    override fun characterEncoding(): String = characterEncoding

    private val cookieStore by lazy { super.cookieStore() }
    override fun cookieStore() = cookieStore

    private val method by lazy { super.method() }
    override fun method(): HandlerType = method

    override fun handlerType(): HandlerType = handlerType
    override fun matchedPath(): String = matchedPath

    /** has to be cached, because we can read input stream only once */
    private val body by lazy { super.bodyAsBytes() }
    override fun bodyAsBytes(): ByteArray = body

    /** using an additional map lazily so no new objects are created whenever ctx.formParam*() is called */
    private val formParams by lazy { super.formParamMap() }
    override fun formParamMap(): Map<String, List<String>> = formParams

    override fun pathParamMap(): Map<String, String> = Collections.unmodifiableMap(pathParamMap)
    override fun pathParam(key: String): String = ContextUtil.pathParamOrThrow(pathParamMap, key, matchedPath)

    /** using an additional map lazily so no new objects are created whenever ctx.formParam*() is called */
    private val queryParams by lazy { super.queryParamMap() }
    override fun queryParamMap(): Map<String, List<String>> = queryParams

    override fun redirect(location: String, httpStatusCode: Int) {
        header(Header.LOCATION, location).status(httpStatusCode)
        if (handlerType() == HandlerType.BEFORE) {
            throw RedirectResponse(httpStatusCode)
        }
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
