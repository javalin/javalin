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

    @Test
    fun `rate limiting kicks in if number of requests exceeds rate limit`() = TestUtil.test(Javalin.create()) { app, http ->
        app.get("/rate-limited") { ctx ->
            RateLimiter(ctx).requestPerSeconds(5)
            ctx.result("Hello, World!")
        }
        app.get("/not-rate-limited") { ctx ->
            ctx.result("Hello, World!")
        }
        repeat(5) { assertThat(http.get("/rate-limited").body).isEqualTo("Hello, World!") }
        assertThat(http.get("/rate-limited").status).isEqualTo(429)
        assertThat(http.get("/rate-limited").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per second.")
        repeat(10) { assertThat(http.get("/not-rate-limited").body).isEqualTo("Hello, World!") }
    }

}
