package io.javalin.plugin.bundled

import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus
import io.javalin.plugin.ContextPlugin
import io.javalin.util.ConcurrencyUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class RateLimitPlugin(userConfig: Consumer<Config>? = null) : ContextPlugin<RateLimitPlugin.Config, RateLimitPlugin.Extension>(userConfig, Config()) {

    private val limiters = ConcurrentHashMap<TimeUnit, RateLimiter>()
    private val executor: ScheduledExecutorService = ConcurrencyUtil.newSingleThreadScheduledExecutor(pluginConfig.executorName)

    override fun createExtension(context: Context): Extension = Extension(context)

    class Config {
        /**
         * Function to extract the rate limit key from the context.
         * Default: ip + method + matched endpoint path
         */
        var keyFunction: (Context) -> String = { ctx ->
            val ip = ctx.header("X-Forwarded-For")?.split(",")?.get(0) ?: ctx.ip()
            val path = ctx.endpoints().matchedHttpEndpoint()?.path ?: ctx.endpoint().path
            ip + ctx.method() + path
        }

        var executorName: String = "JavalinRateLimitExecutor"
    }

    /**
     * Extension methods added to Context when using this plugin.
     */
    inner class Extension(private val context: Context) {

        /**
         * Naive in-memory key/count rate-limiting.
         * All counters are cleared every [timeUnit].
         * @param numRequests Maximum number of requests allowed per time unit
         * @param timeUnit The time unit for rate limiting
         * @throws io.javalin.http.HttpResponseException if the counter exceeds [numRequests] per [timeUnit]
         */
        fun requestPerTimeUnit(numRequests: Int, timeUnit: TimeUnit) {
            limiters.computeIfAbsent(timeUnit) { RateLimiter(timeUnit, executor, pluginConfig.keyFunction) }
                .incrementCounter(context, numRequests)
        }

        /**
         * Check the current count for this context without incrementing.
         * @param timeUnit The time unit to check
         * @return The current request count, or 0 if not tracked
         */
        fun getCurrentCount(timeUnit: TimeUnit): Int {
            return limiters[timeUnit]?.getCurrentCount(context) ?: 0
        }
    }

    /**
     * Internal rate limiter for a specific time unit.
     */
    internal class RateLimiter(
        private val timeUnit: TimeUnit,
        executor: ScheduledExecutorService,
        private val keyFunction: (Context) -> String
    ) {
        private val timeUnitString = timeUnit.toString().lowercase(Locale.ROOT).removeSuffix("s")
        private val keyToRequestCount = ConcurrentHashMap<String, Int>().also {
            executor.scheduleAtFixedRate({ it.clear() }, /*delay=*/1, /*period=*/1, timeUnit)
        }

        fun incrementCounter(ctx: Context, requestLimit: Int) {
            keyToRequestCount.compute(keyFunction(ctx)) { _, count ->
                when {
                    count == null -> 1
                    count < requestLimit -> count + 1
                    else -> throw HttpResponseException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limit exceeded - Server allows $requestLimit requests per $timeUnitString."
                    )
                }
            }
        }

        fun getCurrentCount(ctx: Context) = keyToRequestCount[keyFunction(ctx)] ?: 0
    }
}
