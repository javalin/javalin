/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Context
import io.javalin.LogLevel
import io.javalin.embeddedserver.CachedResponseWrapper
import org.slf4j.Logger
import java.util.*

object LogUtil {

    fun logRequestAndResponse(ctx: Context, logLevel: LogLevel, log: Logger) {
        val startTime: Long = ctx.attribute("javalin-request-log-start-time")
        val executionTime = Formatter(Locale.US).format("%.2f", (System.nanoTime() - startTime) / 1000000f)
        with(ctx) {
            val resContentType = response().contentType ?: "content-type-not-set"
            when (logLevel) {
                LogLevel.OFF -> false
                LogLevel.MINIMAL -> log.info("${method()} -> ${status()} ($executionTime ms)")
                LogLevel.STANDARD -> log.info("${method()} ${path()} -> ${status()} [$resContentType] (took $executionTime ms)")
                LogLevel.EXTENSIVE -> {
                    val resBody = (response() as CachedResponseWrapper).getCopy()
                    val resHeaders = response().headerNames.asSequence().map { it to response().getHeader(it) }.toMap()
                    log.info("""JAVALIN EXTENSIVE REQUEST LOG (this clones the response, which is an expensive operation):
                        |Request: ${method()} [${path()}]
                        |    Headers: ${headerMap()}
                        |    Cookies: ${cookieMap()}
                        |    Body: ${if (ctx.isMultipart()) "Multipart data ..." else body()}
                        |    QueryString: ${queryString()}
                        |    QueryParams: ${queryParamMap().mapValues { (_, v) -> v.contentToString() }}
                        |    FormParams: ${formParamMap().mapValues { (_, v) -> v.contentToString() }}
                        |Response: [${status()}], execution took $executionTime ms
                        |    Headers: ${resHeaders}
                        |    Body: (starts on next line)
                        |${if (resBody.isNotEmpty()) resBody else "No body was set"}
                        |----------------------------------------------------------------------------------""".trimMargin())
                }
            }
        }
    }

}
