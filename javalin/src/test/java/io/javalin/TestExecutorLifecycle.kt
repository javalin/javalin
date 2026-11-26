/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.http.util.AsyncExecutor
import io.javalin.http.util.AsyncExecutor.Companion.AsyncExecutorKey
import io.javalin.plugin.bundled.RateLimitPlugin
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import io.javalin.util.javalinLazy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@Timeout(value = 15, unit = SECONDS)
class TestExecutorLifecycle {

    @Test
    fun `repeated create start stop cycles do not accumulate async or rate limit threads`() = TestUtil.runLogLess {
        val observedAsyncThreads = ConcurrentHashMap.newKeySet<Thread>()
        val rateLimitPrefix = "TestLifecycleRateLimit-${System.nanoTime()}-"

        repeat(5) { cycle ->
            val cycleRateLimitPrefix = "$rateLimitPrefix$cycle"
            val app = Javalin.create { config ->
                config.registerPlugin(RateLimitPlugin { it.executorName = cycleRateLimitPrefix })
                config.routes.get("/async") { ctx ->
                    ctx.async {
                        observedAsyncThreads.add(Thread.currentThread())
                        ctx.result("async-$cycle")
                    }
                }
                config.routes.get("/limited") { ctx ->
                    ctx.with(RateLimitPlugin::class).requestPerTimeUnit(10, TimeUnit.SECONDS)
                    ctx.result("limited-$cycle")
                }
            }.start(0)

            val http = HttpUtil(app.port())
            assertThat(http.get("/async").httpCode()).isEqualTo(OK)
            assertThat(http.getBody("/limited")).isEqualTo("limited-$cycle")
            assertThat(liveThreadsWithPrefix(cycleRateLimitPrefix)).isNotEmpty()

            app.stop()

            awaitCondition { liveThreadsWithPrefix(cycleRateLimitPrefix).isEmpty() }
            assertThat(liveThreadsWithPrefix(cycleRateLimitPrefix)).isEmpty()
        }

        awaitCondition { observedAsyncThreads.none { it.isAlive } }
        assertThat(observedAsyncThreads).isNotEmpty()
        assertThat(observedAsyncThreads).noneMatch { it.isAlive }
    }

    @Test
    fun `custom async executor in ConcurrencyConfig is not shut down on stop`() = TestUtil.runLogLess {
        val customExecutor = Executors.newSingleThreadExecutor()
        try {
            val app = Javalin.create { config ->
                config.concurrency.executor = javalinLazy { AsyncExecutor(customExecutor) }
                config.routes.get("/async") { ctx ->
                    ctx.async { ctx.result("custom") }
                }
            }.start(0)

            assertThat(HttpUtil(app.port()).getBody("/async")).isEqualTo("custom")
            app.stop()

            assertThat(customExecutor.isShutdown).isFalse()
            assertThat(customExecutor.isTerminated).isFalse()
            assertThat(customExecutor.submit<String> { "still-alive" }.get(2, SECONDS)).isEqualTo("still-alive")
        } finally {
            customExecutor.shutdownNow()
        }
    }

    @Test
    fun `custom async executor in appData is not shut down on stop`() = TestUtil.runLogLess {
        val customExecutor = Executors.newSingleThreadExecutor()
        try {
            val app = Javalin.create { config ->
                config.appData(AsyncExecutorKey, AsyncExecutor(customExecutor))
                config.routes.get("/async") { ctx ->
                    ctx.async { ctx.result("appdata") }
                }
            }.start(0)

            assertThat(HttpUtil(app.port()).getBody("/async")).isEqualTo("appdata")
            app.stop()

            assertThat(customExecutor.isShutdown).isFalse()
            assertThat(customExecutor.isTerminated).isFalse()
        } finally {
            customExecutor.shutdownNow()
        }
    }

    @Test
    fun `per-task custom executor is not shut down on stop`() = TestUtil.runLogLess {
        val customExecutor = Executors.newSingleThreadExecutor()
        try {
            val app = Javalin.create { config ->
                config.routes.get("/async") { ctx ->
                    ctx.async({ it.executor = customExecutor }) {
                        ctx.result("per-task")
                    }
                }
            }.start(0)

            assertThat(HttpUtil(app.port()).getBody("/async")).isEqualTo("per-task")
            app.stop()

            assertThat(customExecutor.isShutdown).isFalse()
            assertThat(customExecutor.isTerminated).isFalse()
        } finally {
            customExecutor.shutdownNow()
        }
    }

    private fun liveThreadsWithPrefix(prefix: String): List<Thread> =
        Thread.getAllStackTraces().keys.filter { it.isAlive && it.name.startsWith(prefix) }

    private fun awaitCondition(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (!condition() && System.nanoTime() < deadline) {
            Thread.sleep(20)
        }
    }
}
