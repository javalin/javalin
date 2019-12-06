/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.RateLimiter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestRateLimitUtil {

    private val testApp by lazy {
        Javalin.create()
                .get("/") { ctx ->
                    RateLimiter(ctx).requestPerSeconds(5)
                    ctx.result("Hello, World!")
                }
                .post("/") { it.result("Hello, World!") }
    }

    @Test
    fun `rate limiting kicks in if number of requests exceeds rate limit`() = TestUtil.test(testApp) { app, http ->
        repeat(10) { http.get("/") }
        assertThat(http.get("/").status).isEqualTo(429)
        assertThat(http.get("/").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per second.")
    }

    @Test
    fun `rate limit doesn't affect other verbs`() = TestUtil.test(testApp) { app, http ->
        repeat(10) { assertThat(http.post("/").asString().body).isEqualTo("Hello, World!") }
    }

    @Test
    fun `rate limit doesn't affect other paths`() = TestUtil.test(testApp) { app, http ->
        repeat(10) { assertThat(http.get("/test").body).isEqualTo("Not found") }
    }

}
