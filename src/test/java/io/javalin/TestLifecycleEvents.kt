/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TestLifecycleEvents {

    @Test
    fun `life cycle events work`() {
        var log = ""
        Javalin.create().apply {
            event(JavalinEvent.SERVER_STARTING) { log += "Starting" }
            event(JavalinEvent.SERVER_STARTED) { log += "Started" }
            event(JavalinEvent.SERVER_STOPPING) { log += "Stopping" }
            event(JavalinEvent.SERVER_STOPPING) { log += "Stopping" }
            event(JavalinEvent.SERVER_STOPPING) { log += "Stopping" }
            event(JavalinEvent.SERVER_STOPPED) { log += "Stopped" }
        }.start(0).stop()
        assertThat(log, `is`("StartingStartedStoppingStoppingStoppingStopped"))
    }

}
