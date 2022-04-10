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
        Javalin.create().events { event ->
            event.serverStarting { log += "Starting" }
            event.serverStarted { log += "Started" }
            event.serverStopping { log += "Stopping" }
            event.serverStopping { log += "Stopping" }
            event.serverStopping { log += "Stopping" }
            event.serverStopped { log += "Stopped" }
        }.start(0).stop()
        assertThat(log).isEqualTo("StartingStartedStoppingStoppingStoppingStopped")
    }

    @Test
    fun `server started event works`() = TestUtil.runLogLess {
        var log = ""
        val existingApp = Javalin.create().start(20000)
        runCatching {
            Javalin.create().events { event ->
                event.serverStartFailed { log += "Failed to start" }
            }.start(20000).stop() // port conflict
        }
        assertThat(log).isEqualTo("Failed to start")
        existingApp.stop()
    }

    @Test
    fun `handlerAdded event works`() = TestUtil.test { app, _ ->
        var log = ""
        app.events { it.handlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path } }
        app.events { it.handlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path } }
        app.get("/test-path") {}
        assertThat(log).isEqualTo("/test-path/test-path")
    }

    @Test
    fun `wsHandlerAdded event works`() = TestUtil.test { app, _ ->
        var log = ""
        app.events { it.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path } }
        app.events { it.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path } }
        app.ws("/test-path-ws") {}
        assertThat(log).isEqualTo("/test-path-ws/test-path-ws")
    }

}
