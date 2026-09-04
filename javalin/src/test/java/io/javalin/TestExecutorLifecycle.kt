/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.plugin.bundled.RateLimitPlugin
import io.javalin.testing.HttpUtil
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import io.javalin.websocket.WsTestClient
import io.javalin.websocket.awaitCondition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Item 3 of #2628: Javalin-owned executors (async / piped / ws-ping / rate-limit)
 * must be disposed on stop so create/start/stop cycles do not leak threads.
 * Neighbour: [TestGracefulShutdown].
 */
@Timeout(value = 30, unit = SECONDS)
internal class TestExecutorLifecycle {

    @Test
    fun `repeated create start stop cycles do not accumulate Javalin executor threads`() = TestUtil.runLogLess {
        val baseline = javalinOwnedExecutorThreads().toSet()
        val leftoverPerCycle = mutableListOf<List<Thread>>()

        repeat(CYCLES) {
            val pings = ConcurrentLinkedQueue<String>()
            val app = Javalin.create { cfg ->
                cfg.registerPlugin(RateLimitPlugin())
                cfg.routes.get("/async") { ctx ->
                    ctx.async { ctx.result("async-ok") }
                }
                cfg.routes.get("/json-stream") { ctx ->
                    ctx.jsonStream(SerializableObject())
                }
                cfg.routes.get("/rate") { ctx ->
                    ctx.with(RateLimitPlugin::class).requestPerTimeUnit(1_000, TimeUnit.HOURS)
                    ctx.result("rate-ok")
                }
                cfg.routes.ws("/ws") { ws ->
                    ws.onConnect { it.enableAutomaticPings(5, TimeUnit.MILLISECONDS) }
                }
            }
            app.start(0)
            val http = HttpUtil(app.port())
            assertThat(http.getBody("/async")).isEqualTo("async-ok")
            assertThat(http.getBody("/json-stream")).contains("value1")
            assertThat(http.getBody("/rate")).isEqualTo("rate-ok")

            val client = WsTestClient(app, "/ws", onPing = { pings.add("ping") })
            client.connectBlocking()
            awaitCondition(condition = { pings.isNotEmpty() })
            client.disconnectBlocking()
            app.stop()

            leftoverPerCycle += awaitExecutorThreadsBeyond(baseline)
        }

        leftoverPerCycle.forEach { leftover ->
            assertThat(leftover.map { it.name })
                .withFailMessage { "Javalin executor threads still alive after stop(): ${leftover.map { it.name }}" }
                .isEmpty()
        }
    }

    companion object {
        private const val CYCLES = 5
        private val EXECUTOR_PREFIXES = listOf(
            "JavalinDefaultAsyncThreadPool",
            "JavalinPipedStreamingThreadPool",
            "JavalinWebSocketPingThread",
            "JavalinRateLimitExecutor",
        )

        private fun javalinOwnedExecutorThreads(): List<Thread> =
            Thread.getAllStackTraces().keys.filter { thread ->
                thread.isAlive && EXECUTOR_PREFIXES.any { thread.name.startsWith(it) }
            }

        private fun awaitExecutorThreadsBeyond(baseline: Set<Thread>): List<Thread> {
            val deadline = System.currentTimeMillis() + 2_000
            var leftover: List<Thread>
            do {
                leftover = javalinOwnedExecutorThreads().filter { it !in baseline }
                if (leftover.isEmpty()) return leftover
                Thread.sleep(20)
            } while (System.currentTimeMillis() < deadline)
            return leftover
        }
    }
}
