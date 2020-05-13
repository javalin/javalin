/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.util.RateLimit
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class TestRateLimitUtil {

    private val testApp by lazy {
        Javalin.create()
                .get("/") { RateLimit(it).requestPerTimeUnit(5, TimeUnit.HOURS) }
                .get("/dynamic/:path") { RateLimit(it).requestPerTimeUnit(5, TimeUnit.MINUTES) }
                .get("/ms") { RateLimit(it).requestPerTimeUnit(1, TimeUnit.MILLISECONDS) }
                .post("/") { }
    }

    @Test
    fun `rate limiting kicks in if number of requests exceeds rate limit`() = TestUtil.test(testApp) { app, http ->
        repeat(10) { http.get("/") }
        assertThat(http.get("/").status).isEqualTo(429)
        assertThat(http.get("/").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per hour.")
    }

    @Test
    fun `both path and HTTP method must match for rate limiting to kick in`() = TestUtil.test(testApp) { app, http ->
        repeat(10) { http.get("/") }
        assertThat(http.get("/").status).isEqualTo(429)
        assertThat(http.get("/test").status).isNotEqualTo(429)
        assertThat(http.post("/").asString().status).isNotEqualTo(429)
    }

    @Test
    fun `rate limit on dynamic path-params limits per endpoint, not per URL`() = TestUtil.test(testApp) { app, http ->
        repeat(5) { http.get("/dynamic/1") }
        repeat(5) { http.get("/dynamic/2") }
        repeat(5) { http.get("/dynamic/3") }
        assertThat(http.get("/dynamic/4").status).isEqualTo(429)
        assertThat(http.get("/dynamic/5").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per minute.")
    }

    @Test
    fun `millisecond rate-limiting works`() = TestUtil.test(testApp) { app, http ->
        repeat(3) {
            Thread.sleep(5)
            assertThat(http.get("/ms").status).isEqualTo(200)
        }
    }

}
