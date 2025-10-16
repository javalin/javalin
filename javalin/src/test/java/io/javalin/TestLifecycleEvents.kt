/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
            config.events { it.handlerAdded { handlerMetaInfo -> routerLog += handlerMetaInfo.path } }
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
            config.events { it.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path } }
            config.events { it.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path } }
            config.routes.ws("/test-path-ws") {}
        }) { _, _ ->
            assertThat(log).isEqualTo("/test-path-ws/test-path-ws")
        }
    }

}
