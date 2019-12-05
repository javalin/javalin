/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A very simple IP-based rate-limiting plugin.
 * A ConcurrentHashMap ([ipRequestCount]) holds IPs and counters, and the rules are:
 * - Incremented counter on every request.
 * - Decremented all counters every [requestsPerSecond] seconds.
 * - Short circuit request if count > [bufferSize]
 */
class RateLimitPlugin(private val bufferSize: Int, private val requestsPerSecond: Int) : Plugin {

    class RateLimitException : Exception()

    private val ipRequestCount = ConcurrentHashMap<String, Int>()

    override fun apply(app: Javalin) {

        app.before { ctx ->
            ipRequestCount.compute(ctx.ip()) { _, count ->
                when (count) {
                    null -> 1
                    in 0 until bufferSize -> count + 1
                    else -> throw RateLimitException()
                }
            }
        }.exception(RateLimitException::class.java) { _, ctx ->
            ctx.status(429).result("Ratelimited - Server allows $requestsPerSecond requests per second with a buffer of $bufferSize.")
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            ipRequestCount.forEachKey(1) { ip ->
                ipRequestCount.computeIfPresent(ip) { _, count -> if (count > 1) count - 1 else null }
            }
        }, 0, requestsPerSecond.toLong(), TimeUnit.SECONDS)

    }
}
