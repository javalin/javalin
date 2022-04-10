/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */
package io.javalin

import io.javalin.core.util.JavalinLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestClose {

    @Test
    fun useStopsServer() {
        JavalinLogger.enabled = false
        val app = Javalin.create()
        app.start(0).use { }
        assertThat(app.jettyServer.server().isStopped).isTrue
        JavalinLogger.enabled = true
    }

    @Test
    fun useCallsLifecycleEvents() {
        JavalinLogger.enabled = false
        var log = ""
        val app = Javalin.create().events {
            it.serverStopping { log += "Stopping" }
            it.serverStopped { log += "Stopped" }
        }
        app.start(0).use { }
        assertThat(log).isEqualTo("StoppingStopped")
        JavalinLogger.enabled = true
    }

    @Test
    fun closingInsideUseIsIdempotent() {
        JavalinLogger.enabled = false
        var log = ""
        val app = Javalin.create().events {
            it.serverStopping { log += "Stopping" }
            it.serverStopped { log += "Stopped" }
        }
        app.start(0).use { it.close() }
        assertThat(app.jettyServer.server().isStopped).isTrue
        assertThat(log).isEqualTo("StoppingStopped")
        JavalinLogger.enabled = true
    }
}
