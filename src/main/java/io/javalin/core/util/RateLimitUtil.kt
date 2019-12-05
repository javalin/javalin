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
    val ipPathRequestCount = ConcurrentHashMap<String, Int>()

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({ ipPathRequestCount.clear() }, 0, 1, TimeUnit.SECONDS)
    }
}

/**
 * Simple IP-and-path-based rate-limiting, activated by calling it in a [io.javalin.http.Handler]
 * A [ConcurrentHashMap] in [RateLimitUtil] holds IP/path keys and counter values.
 * The [RateLimiter] works by incrementing counters when less than requestsPerSecond,
 * and throwing if not. The map is cleared every second.
 */
class RateLimiter(val ctx: Context) {
    fun requestPerSeconds(requestsPerSecond: Int) {
        val mapKey = ctx.ip() + ctx.matchedPath() // rate-limit on ip + handler path
        RateLimitUtil.ipPathRequestCount.compute(mapKey) { _, count ->
            when {
                count == null -> 1
                count < requestsPerSecond -> count + 1
                else -> throw HttpResponseException(429, "Rate limit exceeded - Server allows $requestsPerSecond requests per second.")
            }
        }
    }
}
