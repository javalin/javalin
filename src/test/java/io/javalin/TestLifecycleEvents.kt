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
            on(JavalinEvent.SERVER_STARTING) { log += "Starting" }
            on(JavalinEvent.SERVER_STARTED) { log += "Started" }
            on(JavalinEvent.SERVER_STOPPING) { log += "Stopping" }
            on(JavalinEvent.SERVER_STOPPING) { log += "Stopping" }
            on(JavalinEvent.SERVER_STOPPING) { log += "Stopping" }
            on(JavalinEvent.SERVER_STOPPED) { log += "Stopped" }
        }.start(0).stop()
        assertThat(log).isEqualTo("StartingStartedStoppingStoppingStoppingStopped")
    }

}
