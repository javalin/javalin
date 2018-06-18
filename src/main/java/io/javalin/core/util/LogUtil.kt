/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Context
import io.javalin.core.CachedResponseWrapper
import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher
import org.slf4j.Logger
import java.util.*

object LogUtil {

    fun logRequestAndResponse(ctx: Context, matcher: PathMatcher, log: Logger, gzipped: Boolean) {
        val type = HandlerType.fromServletRequest(ctx.request())
        val requestUri = ctx.request().requestURI
        val executionTime = Formatter(Locale.US).format("%.2f", executionTime(ctx))
        with(ctx) {
            val allMatching = (matcher.findEntries(HandlerType.BEFORE, requestUri) + matcher.findEntries(type, requestUri) + matcher.findEntries(HandlerType.AFTER, requestUri)).map { it.type.name + "=" + it.path }
            val resBody = (response() as CachedResponseWrapper).getCopy()
            val resHeaders = response().headerNames.asSequence().map { it to response().getHeader(it) }.toMap()
            log.info("""JAVALIN DEBUG REQUEST LOG (this clones the response, which is an expensive operation):
                        |Request: ${method()} [${path()}]
                        |    Matching endpoint-handlers: $allMatching
                        |    Headers: ${headerMap()}
                        |    Cookies: ${cookieMap()}
                        |    Body: ${if (ctx.isMultipart()) "Multipart data ..." else body()}
                        |    QueryString: ${queryString()}
                        |    QueryParams: ${queryParamMap().mapValues { (_, v) -> v.toString() }}
                        |    FormParams: ${formParamMap().mapValues { (_, v) -> v.toString() }}
                        |Response: [${status()}], execution took $executionTime ms
                        |    Headers: $resHeaders
                        |    Body: ${resBody.length} bytes (starts on next line)
                        |${if (resBody.isNotEmpty()) (if (gzipped) "dynamically gzipped response ..." else resBody) else "No body was set"}
                        |----------------------------------------------------------------------------------""".trimMargin())
        }
    }

    fun executionTime(ctx: Context) = (System.nanoTime() - ctx.attribute("javalin-request-log-start-time") as Long) / 1000000f
}

