/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object RateLimitUtil {
    val handlerToIpToRequestCount = HashMap<String, ConcurrentHashMap<String, Int>>()

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            handlerToIpToRequestCount.forEach { (_, ipRequestCount) -> ipRequestCount.clear() }
        }, /*delay=*/0,  /*period=*/1, TimeUnit.MINUTES)
    }

}

/**
 * Simple IP-and-handler-based rate-limiting, activated by calling it in a [io.javalin.http.Handler]
 * A map of maps in [RateLimitUtil] holds one ip/counter map per method/path (handler).
 * On each request the counter for that IP is incremented. If the counter exceeds number of
 * requests per minute, an exception is thrown. All counters are cleared every minute.
 */
class RateLimiter(val ctx: Context) {

    fun requestsPerSeconds(requestsPerSecond: Int) = requestsPerMinute(requestsPerSecond * 60)

    fun requestsPerMinute(requestsPerMinute: Int) {
        val limiter = ctx.method() + ctx.matchedPath()
        RateLimitUtil.handlerToIpToRequestCount.putIfAbsent(limiter, ConcurrentHashMap())
        RateLimitUtil.handlerToIpToRequestCount[limiter]!!.compute(ctx.ip()) { _, count ->
            when {
                count == null -> 1
                count < requestsPerMinute -> count + 1
                else -> throw HttpResponseException(429, "Rate limit exceeded - Server allows $requestsPerMinute requests per minute.")
            }
        }
    }

}
