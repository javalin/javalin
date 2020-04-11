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

class RateLimiter(timeUnit: TimeUnit) {

    private val handlerToIpToRequestCount = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            handlerToIpToRequestCount.forEach { (_, ipRequestCount) -> ipRequestCount.clear() }
        }, /*delay=*/0,  /*period=*/1, timeUnit)
    }

    fun incrementCounter(ctx: Context, requestLimit: Int) {
        val limiterName = ctx.method() + ctx.matchedPath()
        handlerToIpToRequestCount.putIfAbsent(limiterName, ConcurrentHashMap())
        handlerToIpToRequestCount[limiterName]!!.compute(ip(ctx)) { _, count ->
            when {
                count == null -> 1
                count < requestLimit -> count + 1
                else -> throw RuntimeException("Too many requests!")
            }
        }
    }

}

private fun ip(ctx: Context) = ctx.header("X-Forwarded-For")?.split(",")?.get(0) ?: ctx.ip()

class RateLimit(private val ctx: Context) {
    /**
     * Simple IP-and-handler-based rate-limiting, activated by calling it in a [io.javalin.http.Handler]
     * A map of maps in a [RateLimiter] holds one ip/counter map per method/path (handler).
     * On each request the counter for that IP is incremented. If the counter exceeds [numRequests]
     * per [timeUnit], an exception is thrown. All counters are cleared every [timeUnit].
     */
    fun requestPerTimeUnit(numRequests: Int, timeUnit: TimeUnit) = try {
        limiters.computeIfAbsent(timeUnit) { RateLimiter(timeUnit) }.incrementCounter(ctx, numRequests)
    } catch (e: RuntimeException) {
        throw HttpResponseException(429, "Rate limit exceeded - Server allows $numRequests requests per ${timeUnit.toString().toLowerCase().removeSuffix("s")}.")
    }
}
