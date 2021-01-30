/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object RateLimitUtil {
    val limiters = ConcurrentHashMap<TimeUnit, RateLimiter>()
    var keyFunction: (Context) -> String = { ip(it) + it.method() + it.matchedPath() }
    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private fun ip(ctx: Context) = ctx.header("X-Forwarded-For")?.split(",")?.get(0) ?: ctx.ip()
}

class RateLimiter(val timeUnit: TimeUnit) {

    private val timeUnitString = timeUnit.toString().toLowerCase().removeSuffix("s")
    private val keyToRequestCount = ConcurrentHashMap<String, Int>().also {
        RateLimitUtil.executor.scheduleAtFixedRate({ it.clear() }, /*delay=*/0,  /*period=*/1, timeUnit)
    }

    fun incrementCounter(ctx: Context, requestLimit: Int) {
        keyToRequestCount.compute(RateLimitUtil.keyFunction(ctx)) { _, count ->
            when {
                count == null -> 1
                count < requestLimit -> count + 1
                else -> throw HttpResponseException(429, "Rate limit exceeded - Server allows $requestLimit requests per $timeUnitString.")
            }
        }
    }
}

class RateLimit(private val ctx: Context) {
    /**
     * Simple key/count rate-limiting, activated by calling it in a [io.javalin.http.Handler].
     * All counters are cleared every [timeUnit].
     * You can change the key by changing [RateLimitUtil.keyFunction], the default is ip + method + path
     * @throws HttpResponseException if the counter exceeds [numRequests] per [timeUnit]
     */
    fun requestPerTimeUnit(numRequests: Int, timeUnit: TimeUnit) {
        RateLimitUtil.limiters.computeIfAbsent(timeUnit) { RateLimiter(timeUnit) }.incrementCounter(ctx, numRequests)
    }
}
