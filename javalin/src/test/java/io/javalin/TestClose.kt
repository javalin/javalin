/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */
package io.javalin

import io.javalin.testing.TestUtil
import io.javalin.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AutoClosableJavalin(val javalin: Javalin) : AutoCloseable {
    override fun close() {
        javalin.stop()
    }

    fun start(port: Int): AutoClosableJavalin = also { javalin.start(port) }
    fun jettyServer() = javalin.jettyServer()
}

class TestClose {

    @Test
    fun useStopsServer() = TestUtil.runLogLess {
        val app = AutoClosableJavalin(Javalin.create())
        app.start(0).use { }
        assertThat(app.jettyServer().server().isStopped).isTrue
    }

    @Test
    fun useCallsLifecycleEvents() = TestUtil.runLogLess {
        var log = ""
        val app = AutoClosableJavalin(Javalin.create().events {
            it.serverStopping { log += "Stopping" }
            it.serverStopped { log += "Stopped" }
        })
        app.start(0).use { }
        assertThat(log).isEqualTo("StoppingStopped")
    }

    @Test
    fun closingInsideUseIsIdempotent() = TestUtil.runLogLess {
        var log = ""
        val app = AutoClosableJavalin(Javalin.create().events {
            it.serverStopping { log += "Stopping" }
            it.serverStopped { log += "Stopped" }
        })
        app.start(0).use { it.close() }
        assertThat(app.jettyServer().server().isStopped).isTrue
        assertThat(log).isEqualTo("StoppingStopped")
    }

}
