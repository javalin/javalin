/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.RateLimitPlugin
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestRateLimitPlugin {

    @Test
    fun `ratelimiting kicks in after buffer is empty`() {

        val rateLimitedJavalin = Javalin.create {
            it.registerPlugin(RateLimitPlugin(bufferSize = 5, requestsPerSecond = 10))
        }.get("/") { it.result("Hello, World!") }

        TestUtil.test(rateLimitedJavalin) { app, http ->
            repeat(5) { assertThat(http.get("/").body).isEqualTo("Hello, World!") }
            assertThat(http.get("/").body).isEqualTo("Ratelimited - Server allows 10 requests per second with a buffer of 5.")
        }

    }

}
