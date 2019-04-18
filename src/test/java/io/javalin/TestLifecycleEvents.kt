/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestLifecycleEvents {

    @Test
    fun `life cycle events work`() {
        var log = ""
        Javalin.create().apply {
            on.serverStarting { log += "Starting" }
            on.serverStarted { log += "Started" }
            on.serverStopping { log += "Stopping" }
            on.serverStopping { log += "Stopping" }
            on.serverStopping { log += "Stopping" }
            on.serverStopped { log += "Stopped" }
        }.start(0).stop()
        assertThat(log).isEqualTo("StartingStartedStoppingStoppingStoppingStopped")
    }

    @Test
    fun `handlerAdded event works`() = TestUtil.test { app, http ->
        var log = ""
        app.on.handlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
        app.on.handlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
        app.get("/test-path") {}
        assertThat(log).isEqualTo("/test-path/test-path")
    }

    @Test
    fun `wsHandlerAdded event works`() = TestUtil.test { app, http ->
        var log = ""
        app.on.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
        app.on.wsHandlerAdded { handlerMetaInfo -> log += handlerMetaInfo.path }
        app.ws("/test-path-ws") {}
        assertThat(log).isEqualTo("/test-path-ws/test-path-ws")
    }

}
