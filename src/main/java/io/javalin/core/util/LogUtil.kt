/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Context
import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher
import io.javalin.websocket.WsSession
import org.slf4j.LoggerFactory
import java.util.*

object LogUtil {

    private val log = LoggerFactory.getLogger(LogUtil::class.java)

    fun logRequestAndResponse(ctx: Context, matcher: PathMatcher) = try {
        val type = HandlerType.fromServletRequest(ctx.req)
        val requestUri = ctx.req.requestURI
        val executionTimeMs = Formatter(Locale.US).format("%.2f", executionTimeMs(ctx))
        val gzipped = ctx.res.getHeader(Header.CONTENT_ENCODING) == "gzip"
        val staticFile = ctx.req.getAttribute("handled-as-static-file") == true
        with(ctx) {
            val allMatching = (matcher.findEntries(HandlerType.BEFORE, requestUri) + matcher.findEntries(type, requestUri) + matcher.findEntries(HandlerType.AFTER, requestUri)).map { it.type.name + "=" + it.path }
            val resBody = resultStream()?.apply { reset() }?.bufferedReader()?.use { it.readText() } ?: ""
            val resHeaders = res.headerNames.asSequence().map { it to res.getHeader(it) }.toMap()
            log.info("""JAVALIN DEBUG REQUEST LOG:
                        |Request: ${method()} [${path()}]
                        |    Matching endpoint-handlers: $allMatching
                        |    Headers: ${headerMap()}
                        |    Cookies: ${cookieMap()}
                        |    Body: ${if (isMultipart()) "Multipart data ..." else body()}
                        |    QueryString: ${queryString()}
                        |    QueryParams: ${queryParamMap().mapValues { (_, v) -> v.toString() }}
                        |    FormParams: ${formParamMap().mapValues { (_, v) -> v.toString() }}
                        |Response: [${status()}], execution took $executionTimeMs ms
                        |    Headers: $resHeaders
                        |    ${resBody(resBody, gzipped, staticFile)}
                        |----------------------------------------------------------------------------------""".trimMargin())
        }
    } catch (e: Exception) {
        log.info("An exception occurred while logging debug-info", e)
    }

    private fun resBody(resBody: String, gzipped: Boolean, staticFile: Boolean) = when {
        staticFile -> "Body is a static file (not logged)"
        resBody.isNotEmpty() && gzipped -> "Body is gzipped (${resBody.length} bytes, not logged)"
        resBody.isNotEmpty() && !gzipped -> "Body is ${resBody.length} bytes (starts on next line):\n    $resBody"
        else -> "No body was set"
    }

    fun startTimer(ctx: Context) = ctx.attribute("javalin-request-log-start-time", System.nanoTime())

    fun executionTimeMs(ctx: Context) = (System.nanoTime() - ctx.attribute<Long>("javalin-request-log-start-time")!!) / 1000000f

    fun logOnConnect(session: WsSession) {
        with(session) {
            log.info("""JAVALIN WEB SOCKET DEBUG LOG: onConnect
                        ${logCommonWsSession(this)}
                        |----------------------------------------------------------------------------------""".trimMargin())
        }
    }

    fun logOnMessage(session: WsSession, message: String) {
        with(session) {
            log.info("""JAVALIN WEB SOCKET DEBUG LOG: onMessage
                        ${logCommonWsSession(this)}
                        |Message:
                        |$message
                        |----------------------------------------------------------------------------------""".trimMargin())
        }
    }

    fun logOnBinaryMessage(session: WsSession, msg: Array<Byte>, offset: Int, length: Int) {
        with(session) {
            log.info("""JAVALIN WEB SOCKET DEBUG LOG: onBinaryMessage
                        ${logCommonWsSession(this)}
                        |Offset: $offset Length: $length
                        |Binary Message:
                        |$msg
                        |----------------------------------------------------------------------------------""".trimMargin())
        }
    }

    fun logOnClose(session: WsSession, statusCode: Int, reason: String?) {
        with(session) {
            log.info("""JAVALIN WEB SOCKET DEBUG LOG: onClose
                        ${logCommonWsSession(this)}
                        |StatusCode: $statusCode
                        |Reason: ${reason ?: "No reason was provided"}
                        |----------------------------------------------------------------------------------""".trimMargin())
        }
    }

    fun logOnError(session: WsSession, throwable: Throwable?) {
        with(session) {
            log.info("""JAVALIN WEB SOCKET DEBUG LOG: onError
                        ${logCommonWsSession(this)}
                        |Throwable:  ${throwable ?: "No throwable was provided"}
                        |----------------------------------------------------------------------------------""".trimMargin())
        }
    }

    private fun logCommonWsSession(session: WsSession): String {
        return with(session) {
            """|
            |Common information:
            |   Session Id: $id
            |   Host: ${host()}
            |   Matched Path: ${matchedPath()}
            |   PathParams: ${pathParamMap()}
            |   QueryParams: ${queryParamMap()}
            |"""
        }
    }
}

