/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.testing.TestUtil
import io.javalin.util.JavalinException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

class TestLifecycleEvents {

    @Test
    fun `lifecycle events work`() = TestUtil.runLogLess {
        var log = ""
        Javalin.create { config ->
            config.events.serverStarting { log += "Starting" }
            config.events.serverStarted { log += "Started" }
            config.events.serverStopping { log += "Stopping" }
            config.events.serverStopping { log += "Stopping" }
            config.events.serverStopping { log += "Stopping" }
            config.events.serverStopped { log += "Stopped" }
        }.start(0).stop()
        assertThat(log).isEqualTo("StartingStartedStoppingStoppingStoppingStopped")
    }

    @Test
    fun `server started event works`() = TestUtil.runLogLess {
        var log = ""
        val existingApp = Javalin.create().start(20000)
        runCatching {
            Javalin.create { config ->
                config.events.serverStartFailed { log += "Failed to start" }
            }.start(20000).stop() // port conflict
        }
        assertThat(log).isEqualTo("Failed to start")
        existingApp.stop()
    }

    @Test
    fun `handlerAdded event works`() {
        var log = ""
        val app = Javalin.create { config ->
            config.events.handlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
            config.events.handlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
            config.routes.get("/test-path") {}
        }
        assertThat(log).isEqualTo("/test-path/test-path")
    }


    @Test
    fun `handlerAdded event works for router`() {
        var routerLog = ""
        TestUtil.test(Javalin.create { config ->
            config.events.handlerAdded { handlerMetaInfo -> routerLog += handlerMetaInfo.path }
            config.routes.get("/test") {}
            config.routes.post("/tast") {}
        }) { app, _ ->
            assertThat(routerLog).isEqualTo("/test/tast")
        }
    }

    @Test
    fun `wsHandlerAdded event works`() {
        var log = ""
        TestUtil.test(Javalin.create { config ->
            config.events.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
            config.events.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
            config.routes.ws("/test-path-ws") {}
        }) { _, _ ->
            assertThat(log).isEqualTo("/test-path-ws/test-path-ws")
        }
    }

    @Test
    fun `second start on the same instance is rejected`() = TestUtil.runLogLess {
        val app = Javalin.create {
            it.jetty.port = 0
            it.startup.startupWatcherEnabled = false
            it.startup.showJavalinBanner = false
        }.start()
        try {
            assertThat(app.jettyServer().started()).isTrue()
            assertThatThrownBy { app.start() }
                .isInstanceOf(JavalinException::class.java)
                .hasMessageContaining("already started")
        } finally {
            app.stop()
        }
    }

    @Test
    fun `concurrent start claims the instance once`() = TestUtil.runLogLess {
        val app = Javalin.create {
            it.jetty.port = 0
            it.startup.startupWatcherEnabled = false
            it.startup.showJavalinBanner = false
        }
        val threads = 8
        val barrier = CyclicBarrier(threads)
        val outcomes = ConcurrentLinkedQueue<Result<Unit>>()
        val workers = (1..threads).map {
            Thread {
                try {
                    barrier.await(5, TimeUnit.SECONDS)
                    app.start()
                    outcomes.add(Result.success(Unit))
                } catch (t: Throwable) {
                    outcomes.add(Result.failure(t))
                }
            }
        }
        workers.forEach { it.start() }
        workers.forEach { it.join(10_000) }
        try {
            val successes = outcomes.count { it.isSuccess }
            val alreadyStarted = outcomes.mapNotNull { it.exceptionOrNull() }
                .filter { it is JavalinException && it.message?.contains("already started") == true }
            assertThat(successes).isEqualTo(1)
            assertThat(alreadyStarted).hasSize(threads - 1)
            assertThat(app.jettyServer().started()).isTrue()
        } finally {
            if (app.jettyServer().started()) {
                app.stop()
            }
        }
    }

}
