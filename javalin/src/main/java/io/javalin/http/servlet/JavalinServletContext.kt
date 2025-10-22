/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.servlet

import io.javalin.compression.CompressedOutputStream
import io.javalin.compression.CompressionStrategy
import io.javalin.config.AppDataManager
import io.javalin.config.JavalinConfig
import io.javalin.config.Key
import io.javalin.config.MultipartConfig
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.Header
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.CONTENT_TOO_LARGE
import io.javalin.json.JsonMapper
import io.javalin.plugin.ContextPlugin
import io.javalin.plugin.PluginManager
import io.javalin.router.Endpoint
import io.javalin.router.Endpoints
import io.javalin.router.PathParams
import io.javalin.security.BasicAuthCredentials
import io.javalin.security.RouteRole
import io.javalin.util.JavalinLogger
import io.javalin.util.javalinLazy
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED

data class JavalinServletContextConfig(
    val appDataManager: AppDataManager,
    val pluginManager: PluginManager,
    val compressionStrategy: CompressionStrategy,
    val requestLoggerEnabled: Boolean,
    val defaultContentType: String,
    val jsonMapper: JsonMapper,
    val strictContentTypes: Boolean,
    val multipartConfig: MultipartConfig
) {
    companion object {
        fun of(cfg: JavalinConfig): JavalinServletContextConfig =
            JavalinServletContextConfig(
                appDataManager = cfg.pvt.appDataManager,
                pluginManager = cfg.pvt.pluginManager,
                compressionStrategy = cfg.pvt.compressionStrategy,
                requestLoggerEnabled = cfg.pvt.requestLogger != null,
                defaultContentType = cfg.http.defaultContentType,
                jsonMapper = cfg.pvt.jsonMapper.value,
                strictContentTypes = cfg.http.strictContentTypes,
                multipartConfig = cfg.jetty.multipartConfig,
            )
    }
}

open class JavalinServletContext(
    private val cfg: JavalinServletContextConfig,
    val tasks: Deque<Task> = ArrayDeque(8),
    var exceptionOccurred: Boolean = false,
    val responseWritten: AtomicBoolean = AtomicBoolean(false),
    private var req: HttpServletRequest,
    private val res: HttpServletResponse,
    private val startTimeNanos: Long? = if (cfg.requestLoggerEnabled) System.nanoTime() else null,
    private var routeRoles: Set<RouteRole> = emptySet(),
    internal var userFutureSupplier: Supplier<out CompletableFuture<*>>? = null,
    private var resultStream: InputStream? = null,
    private var minSizeForCompression: Int = cfg.compressionStrategy.defaultMinSizeForCompression,
) : Context {

    private val endpoints: Endpoints = Endpoints()

    init {
        contentType(cfg.defaultContentType)
    }

    fun executionTimeMs(): Float = if (startTimeNanos == null) -1f else (System.nanoTime() - startTimeNanos) / 1000000f

    fun update(endpoint: Endpoint) = also {
        endpoints.add(endpoint)
    }

    override fun req(): HttpServletRequest = req
    override fun res(): HttpServletResponse = res

    override fun <T> appData(key: Key<T>): T = cfg.appDataManager.get(key)

    override fun <T> with(clazz: Class<out ContextPlugin<*, T>>) = cfg.pluginManager.getContextPlugin(clazz).createExtension(this)

    override fun jsonMapper(): JsonMapper = cfg.jsonMapper

    override fun multipartConfig(): MultipartConfig = cfg.multipartConfig

    private val characterEncoding by javalinLazy { super.characterEncoding() ?: "UTF-8" }
    override fun characterEncoding(): String = characterEncoding

    private val cookieStore by javalinLazy(PUBLICATION) { super.cookieStore() }
    override fun cookieStore() = cookieStore

    private val method by javalinLazy { super.method() }
    override fun method(): HandlerType = method

    override fun endpoints(): Endpoints = endpoints

    override fun endpoint(): Endpoint = endpoints.current()

    /** has to be cached, because we can read input stream only once */
    private val body by javalinLazy(SYNCHRONIZED) { super.bodyAsBytes() }
    override fun bodyAsBytes(): ByteArray = body

    /** using an additional map lazily so no new objects are created whenever ctx.formParam*() is called */
    private val formParams by javalinLazy { super.formParamMap() }
    override fun formParamMap(): Map<String, List<String>> = formParams

    override fun strictContentTypes(): Boolean {
        return cfg.strictContentTypes
    }

    override fun pathParamMap(): Map<String, String> {
        val (_, pathParams) = lastEndpointWithPathParams()
        return Collections.unmodifiableMap(pathParams)
    }

    override fun pathParam(key: String): String {
        val (endpoint, pathParams) = lastEndpointWithPathParams()
        return pathParamOrThrow(pathParams, key, endpoint?.path ?: "")
    }

    private fun lastEndpointWithPathParams(): Pair<Endpoint?, Map<String, String>> {
        val endpoint = endpoints.list().findLast { it.metadata(PathParams::class.java)?.params?.isNotEmpty() == true }
        val params = endpoint?.metadata(PathParams::class.java)?.params ?: emptyMap()
        return endpoint to params
    }

    /** using an additional map lazily so no new objects are created whenever ctx.formParam*() is called */
    private val queryParams by javalinLazy { super.queryParamMap() }
    override fun queryParamMap(): Map<String, List<String>> = queryParams

    internal val outputStreamWrapper = javalinLazy(SYNCHRONIZED) {
        CompressedOutputStream(minSizeForCompression, cfg.compressionStrategy, this)
    }

    override fun outputStream(): ServletOutputStream = outputStreamWrapper.value

    override fun minSizeForCompression(minSizeForCompression: Int) = also {
        this.minSizeForCompression = minSizeForCompression
    }

    override fun redirect(location: String, status: HttpStatus) {
        header(Header.LOCATION, location).status(status).result("Redirected")
        if (endpoint().method == HandlerType.BEFORE) {
            tasks.removeIf { it.skipIfExceptionOccurred }
        }
    }

    override fun result(resultStream: InputStream): Context = apply {
        runCatching { this.resultStream?.close() } // avoid memory leaks for multiple result() calls
        this.resultStream = resultStream
    }

    override fun resultInputStream(): InputStream? = resultStream

    override fun future(future: Supplier<out CompletableFuture<*>>) {
        if (userFutureSupplier != null) throw IllegalStateException("Cannot override future from the same handler")
        userFutureSupplier = future
    }

    override fun skipRemainingHandlers(): Context = also {
        tasks.clear()
    }

    override fun routeRoles() = routeRoles
    internal fun setRouteRoles(routeRoles: Set<RouteRole>) {
        this.routeRoles = routeRoles
    }

    override fun writeJsonStream(stream: Stream<*>) {
        minSizeForCompression = 0
        cfg.jsonMapper.writeToOutputStream(stream, this.contentType(ContentType.APPLICATION_JSON).outputStream())
    }
}

// this header is semicolon separated, like: "text/html; charset=UTF-8"
fun getRequestCharset(ctx: Context) = ctx.req().getHeader(Header.CONTENT_TYPE)?.let { value ->
    value.split(";").find { it.trim().startsWith("charset", ignoreCase = true) }?.let { it.split("=")[1].trim().removeSurrounding("\"") }
}

fun splitKeyValueStringAndGroupByKey(string: String, charset: String): Map<String, List<String>> =
    if (string.isEmpty()) mapOf() else string.split("&")
        .map { it.split("=", limit = 2).let { it.get(0) to it.getOrElse(1) { "" } } } // map missing values to empty strings
        .groupBy({ it.first.urlDecode(charset) }, { it.second.urlDecode(charset) })
        .mapNotNull { (k, v) -> k?.let { it to v.filterNotNull() } }.toMap()

private fun String.urlDecode(charset: String): String? =
    runCatching { URLDecoder.decode(this, charset) }.getOrNull()

fun pathParamOrThrow(pathParams: Map<String, String?>, key: String, url: String) =
    pathParams[key.replaceFirst("{", "").replaceFirst("}", "")] ?: throw IllegalArgumentException("'$key' is not a valid path-param for '$url'.")

fun urlDecode(s: String): String = URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+")

/**
 * @throws IllegalStateException if specified string is not valid Basic auth header
 */
fun getBasicAuthCredentials(authorizationHeader: String?): BasicAuthCredentials? =
    authorizationHeader
        ?.takeIf { authorizationHeader.startsWith("Basic ") }
        ?.removePrefix("Basic ")
        ?.let { Base64.getDecoder().decode(it).decodeToString() }
        ?.split(':', limit = 2)
        ?.let { (username, password) -> BasicAuthCredentials(username, password) }

fun acceptsHtml(ctx: Context) =
    ctx.header(Header.ACCEPT)?.contains(ContentType.HTML) == true

fun Context.isLocalhost() = try {
    URI.create(this.url()).toURL().host.let { it == "localhost" || it == "127.0.0.1" }
} catch (_: Exception) {
    false
}

internal object MaxRequestSize {
    val MaxRequestSizeKey = Key<Long>("javalin-max-request-size")

    fun throwContentTooLargeIfContentTooLarge(ctx: Context) {
        val maxRequestSize = ctx.appData(MaxRequestSizeKey)

        if (ctx.req().contentLength > maxRequestSize) {
            JavalinLogger.warn("Body greater than max size ($maxRequestSize bytes)")
            throw HttpResponseException(CONTENT_TOO_LARGE, CONTENT_TOO_LARGE.message)
        }
    }
}

const val SESSION_CACHE_KEY_PREFIX = "javalin-session-attribute-cache-"

fun cacheAndSetSessionAttribute(key: String, value: Any?, req: HttpServletRequest) {
    req.setAttribute("$SESSION_CACHE_KEY_PREFIX$key", value)
    req.session.setAttribute(key, value)
}

fun <T> getCachedRequestAttributeOrSessionAttribute(key: String, req: HttpServletRequest): T? {
    val cachedAttribute = req.getAttribute("$SESSION_CACHE_KEY_PREFIX$key")
    if (cachedAttribute == null) {
        req.setAttribute("$SESSION_CACHE_KEY_PREFIX$key", req.session.getAttribute(key))
    }
    @Suppress("UNCHECKED_CAST")
    return req.getAttribute("$SESSION_CACHE_KEY_PREFIX$key") as T?
}

fun <T> cachedSessionAttributeOrCompute(callback: (Context) -> T, key: String, ctx: Context): T? {
    if (getCachedRequestAttributeOrSessionAttribute<T?>(key, ctx.req()) == null) {
        cacheAndSetSessionAttribute(key, callback(ctx), ctx.req()) // set new value from callback
    }
    return getCachedRequestAttributeOrSessionAttribute(key, ctx.req()) // existing or computed (or null)
}

fun <T> attributeOrCompute(callback: (Context) -> T, key: String, ctx: Context): T? {
    if (ctx.attribute<T>(key) == null) {
        ctx.attribute(key, callback(ctx))
    }
    return ctx.attribute(key)
}

fun readAndResetStreamIfPossible(stream: InputStream?, charset: Charset) = try {
    stream?.apply { reset() }?.readBytes()?.toString(charset).also { stream?.reset() }
} catch (_: Exception) {
    "resultString unavailable (resultStream couldn't be reset)"
}
