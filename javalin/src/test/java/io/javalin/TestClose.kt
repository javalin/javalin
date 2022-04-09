/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */
package io.javalin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestClose {

    @Test
    fun useStopsServer() {
        val app = Javalin.create()
        app.start(0).use { }
        assertThat(app.jettyServer.server().isStopped).isTrue
    }

    @Test
    fun useCallsLifecycleEvents() {
        var log = ""
        val app = Javalin.create().events {
            it.serverStopping { log += "Stopping" }
            it.serverStopped { log += "Stopped" }
        }
        app.start(0).use { }
        assertThat(log).isEqualTo("StoppingStopped")
    }

    @Test
    fun closingInsideUseIsIdempotent() {
        var log = ""
        val app = Javalin.create().events {
            it.serverStopping { log += "Stopping" }
            it.serverStopped { log += "Stopped" }
        }
        app.start(0).use { it.close() }
        assertThat(app.jettyServer.server().isStopped).isTrue
        assertThat(log).isEqualTo("StoppingStopped")
    }
}
