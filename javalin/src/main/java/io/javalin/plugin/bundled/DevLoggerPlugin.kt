package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.Header
import io.javalin.plugin.JavalinPlugin
import io.javalin.router.InternalRouter
import io.javalin.util.JavalinLogger
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsContext
import java.util.*
import java.util.stream.Stream

internal open class DevLoggingPlugin : JavalinPlugin {

    companion object {
        object DevLogging : DevLoggingPlugin()
    }

    override fun onInitialize(config: JavalinConfig) {
        config.requestLogger.http { ctx, ms -> requestDevLogger(config.pvt.internalRouter, ctx, ms) }
        config.requestLogger.ws { wsDevLogger(it) }
    }

    override fun onStart(app: Javalin) {
        app.events { on ->
            on.handlerAdded { handlerMetaInfo ->
                JavalinLogger.info("JAVALIN HANDLER REGISTRATION DEBUG LOG: ${handlerMetaInfo.httpMethod}[${handlerMetaInfo.path}]")
            }
        }
    }
}

fun requestDevLogger(router: InternalRouter, ctx: Context, time: Float) = try {
    val requestUri = ctx.path()
    with(ctx) {
        val allMatching = Stream.of(
                router.findHttpHandlerEntries(HandlerType.BEFORE, requestUri),
                router.findHttpHandlerEntries(ctx.method(), requestUri),
                router.findHttpHandlerEntries(HandlerType.AFTER, requestUri)
            )
            .flatMap { it }
            .map { it.type.name + "=" + it.path }
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

fun wsDevLogger(ws: WsConfig) {
    ws.onConnect { ctx -> ctx.logEvent("onConnect") }
    ws.onMessage { ctx -> ctx.logEvent("onMessage", "Message (next line):\n${ctx.message()}") }
    ws.onBinaryMessage { ctx -> ctx.logEvent("onBinaryMessage", "Offset: ${ctx.offset()}, Length: ${ctx.length()}\nMessage (next line):\n${ctx.data()}") }
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

