package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.Header
import io.javalin.plugin.Plugin
import io.javalin.router.InternalRouter
import io.javalin.util.JavalinLogger
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * The development debugging logger catches most of the interesting stuff about requests and responses,
 * and logs it in an easy-to-read manner. It works both for HTTP and WebSocket requests. Only intended
 * for use during development and/or debugging.
 */
class DevLoggingPlugin(userConfig: Consumer<Config>? = null) : Plugin<DevLoggingPlugin.Config>(userConfig, Config()) {

    class Config {
        @JvmField
        var skipStaticFiles = false
    }

    override fun onInitialize(config: JavalinConfig) {
        config.requestLogger.http { ctx, ms -> httpDevLogger(config.pvt.internalRouter, ctx, ms) }
        config.requestLogger.ws { wsDevLogger(it) }
        config.events.handlerAdded { handlerMetaInfo ->
            JavalinLogger.info("JAVALIN HANDLER REGISTRATION DEBUG LOG: ${handlerMetaInfo.httpMethod}[${handlerMetaInfo.path}]")
        }
    }

    private fun httpDevLogger(router: InternalRouter, ctx: Context, time: Float) {
        if (pluginConfig.skipStaticFiles && isProbablyStaticFile(ctx)) return
        try {
            val requestUri = ctx.path()
            with(ctx) {
                val allMatching = Stream.of(
                        router.findHttpHandlerEntries(HandlerType.BEFORE, requestUri),
                        router.findHttpHandlerEntries(ctx.method(), requestUri),
                        router.findHttpHandlerEntries(HandlerType.AFTER, requestUri)
                    )
                    .flatMap { it }
                    .map { it.endpoint.method.name + "=" + it.endpoint.path }
                val resHeaders = res().headerNames.asSequence().map { it to res().getHeader(it) }.toMap()
                JavalinLogger.info(
                    """|JAVALIN REQUEST DEBUG LOG:
               |Request: ${method()} [${path()}]
               |    Matching endpoint-handlers: $allMatching
               |    Headers: ${headerMap()}
               |    Cookies: ${cookieMap()}
               |    Body: ${if (isMultipart()) "Multipart data ..." else body()}
               |    QueryString: ${queryString()}
               |    QueryParams: ${queryParamMap().mapValues { (_, v) -> v.toString() }}
               |    FormParams: ${(if (body().probablyFormData()) formParamMap() else mapOf()).mapValues { (_, v) -> v.toString() }}
               |Response: [${status()}], execution took ${Formatter(Locale.US).format("%.2f", time)} ms
               |    Headers: $resHeaders
               |    ${resBody(ctx)}
               |----------------------------------------------------------------------------------""".trimMargin()
                )
            }
        } catch (e: Exception) {
            JavalinLogger.info("An exception occurred while logging debug-info", e)
        }
    }

    private fun isProbablyStaticFile(ctx: Context): Boolean {
        val popularExtensions = arrayOf(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg", ".webp",
            ".css",
            ".js", ".mjs",
            ".ttf", ".otf", ".woff", ".woff2",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp3", ".wav", ".ogg",
            ".mp4", ".webm", ".ogv",
            ".ico",
            ".xml", ".json",
            ".zip", ".gz",
            ".map",
        )
        val hasPopularExtension = popularExtensions.any { ctx.path().endsWith(it, ignoreCase = true) }
        val hasExtension = ctx.path().contains(".")
        val hasNoPathParams = ctx.pathParamMap().isEmpty()
        val hasEtag = ctx.header(Header.IF_NONE_MATCH) != null
        val isGetOrHead = ctx.method() == HandlerType.GET || ctx.method() == HandlerType.HEAD
        return hasPopularExtension || hasExtension && hasNoPathParams && hasEtag && isGetOrHead
    }

    private fun String.probablyFormData() = this.trim().firstOrNull()?.isLetter() == true && this.split("=").size >= 2

    private fun resBody(ctx: Context): String {
        val stream = ctx.resultInputStream() ?: return "No body was set"
        if (!stream.markSupported()) {
            return "Body is binary (not logged)"
        }

        val gzipped = ctx.res().getHeader(Header.CONTENT_ENCODING) == "gzip"
        val brotlied = ctx.res().getHeader(Header.CONTENT_ENCODING) == "br"
        val resBody = ctx.result()!!
        return when {
            gzipped -> "Body is gzipped (${resBody.length} bytes, not logged)"
            brotlied -> "Body is brotlied (${resBody.length} bytes, not logged)"
            resBody.contains("resultString unavailable") -> "Body is an InputStream which can't be reset, so it can't be logged"
            else -> "Body is ${resBody.length} bytes (starts on next line):\n    $resBody"
        }
    }

    private fun wsDevLogger(ws: WsConfig) {
        ws.onConnect { ctx -> ctx.logEvent("onConnect") }
        ws.onMessage { ctx -> ctx.logEvent("onMessage", "Message (next line):\n${ctx.message()}") }
        ws.onBinaryMessage { ctx -> ctx.logEvent("onBinaryMessage", "Message (next line):\n${ctx.data()}") }
        ws.onClose { ctx -> ctx.logEvent("onClose", "StatusCode: ${ctx.status()}\nReason: ${ctx.reason() ?: "No reason was provided"}") }
        ws.onError { ctx -> ctx.logEvent("onError", "Throwable:  ${ctx.error() ?: "No throwable was provided"}") }
    }

    private fun WsContext.logEvent(event: String, additionalInfo: String = "") {
        JavalinLogger.info(
            """|JAVALIN WEBSOCKET DEBUG LOG
           |WebSocket Event: $event
           |Session Id: ${this.sessionId()}
           |Host: ${this.host()}
           |Matched Path: ${this.matchedPath()}
           |PathParams: ${this.pathParamMap()}
           |QueryParams: ${if (this.queryString() != null) this.queryParamMap().mapValues { (_, v) -> v.toString() }.toString() else "No query string was provided"}
           |$additionalInfo
           |----------------------------------------------------------------------------------""".trimMargin()
        )
    }

}
