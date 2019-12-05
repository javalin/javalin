/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.SimpleRateLimitPlugin
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestRateLimitPlugin {

    @Test
    fun `rate limiting kicks in if number of requests exceeds rate limit`() {

        val rateLimitedJavalin = Javalin.create {
            it.registerPlugin(SimpleRateLimitPlugin(requestsPerSecond = 5))
        }.get("/") { it.result("Hello, World!") }

        TestUtil.test(rateLimitedJavalin) { app, http ->
            repeat(5) { assertThat(http.get("/").body).isEqualTo("Hello, World!") }
            assertThat(http.get("/").body).isEqualTo("Ratelimited - Server allows 5 requests per second.")
        }

    }

}
