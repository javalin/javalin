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
import java.util.concurrent.TimeUnit

private val limiters = ConcurrentHashMap<TimeUnit, RateLimiter>()

class RateLimiter(val timeUnit: TimeUnit) {

    private val handlerToIpToRequestCount = ConcurrentHashMap<String, Int>()

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            handlerToIpToRequestCount.clear()
        }, /*delay=*/0,  /*period=*/1, timeUnit)
    }

    fun incrementCounter(ctx: Context, requestLimit: Int) {
        val limiterName = ip(ctx) + ctx.method() + ctx.matchedPath()

        handlerToIpToRequestCount.compute(limiterName) { _, count ->
            when {
                count == null -> 1
                count < requestLimit -> count + 1
                else -> throw HttpResponseException(
                    429,
                    "Rate limit exceeded - Server allows $requestLimit requests per ${
                        timeUnit.toString().toLowerCase().removeSuffix("s")
                    }."
                )
            }
        }
    }
}

private fun ip(ctx: Context) = ctx.header("X-Forwarded-For")?.split(",")?.get(0) ?: ctx.ip()

class RateLimit(private val ctx: Context) {
    /**
     * Simple IP-and-handler-based rate-limiting, activated by calling it in a [io.javalin.http.Handler].
     * All counters are cleared every [timeUnit].
     * @throws HttpResponseException if the counter exceeds [numRequests] per [timeUnit]
     */
    fun requestPerTimeUnit(numRequests: Int, timeUnit: TimeUnit) {
        limiters.computeIfAbsent(timeUnit) { RateLimiter(timeUnit) }.incrementCounter(ctx, numRequests)
    }
}
