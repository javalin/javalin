/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.core.util.JavalinException
import io.javalin.jetty.JettyServer
import io.javalin.testing.TestUtil
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestLifecycleEvents {

    @Test
    fun `life cycle events work`() {
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
    fun `serverStartFailed event works`() = `test failed lifecycle event`("StartingStartFailed") {
        every { start(any()) } throws RuntimeException("Lifecycle test exception")
    }

    @Test
    fun `serverStopFailed event works`() = `test failed lifecycle event`("StartingStartedStoppingStopFailed") {
        every { server().stop() } throws RuntimeException("Lifecycle test exception")
    }

    private fun `test failed lifecycle event`(expected: String, mockBlock: JettyServer.() -> Unit) {
        val jettyServer = mockk<JettyServer>(relaxed = true)
        mockBlock(jettyServer)
        val app = Javalin(jettyServer, null)
        var log = ""
        app.events {
            it.serverStartFailed { log += "StartFailed" }
            it.serverStopFailed { log += "StopFailed" }
            it.serverStarting { log += "Starting" }
            it.serverStarted { log += "Started" }
            it.serverStopping { log += "Stopping" }
            it.serverStopped { log += "Stopped" }
        }

        val exception = assertThrows<JavalinException> { app.start(0).stop() }

        assertThat(log).isEqualTo(expected)
        assertThat(exception.cause).isInstanceOf(RuntimeException::class.java)
        assertThat(exception.cause!!.message).isEqualTo("Lifecycle test exception")
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
