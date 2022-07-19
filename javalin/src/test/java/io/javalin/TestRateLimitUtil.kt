/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.*
import io.javalin.http.util.NaiveRateLimit
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TestRateLimitUtil {

    private val testApp by lazy {
        Javalin.create()
            .get("/") { NaiveRateLimit.requestPerTimeUnit(it, 5, TimeUnit.HOURS) }
            .get("/dynamic/{path}") { NaiveRateLimit.requestPerTimeUnit(it, 5, TimeUnit.MINUTES) }
            .get("/ms") { NaiveRateLimit.requestPerTimeUnit(it, 1, TimeUnit.MILLISECONDS) }
            .post("/") { }
    }

    @Test
    fun `rate limiting kicks in if number of requests exceeds rate limit`() = TestUtil.test(testApp) { _, http ->
        repeat(50) { http.get("/") }
        assertThat(http.get("/").status).isEqualTo(TOO_MANY_REQUESTS.status)
        assertThat(http.get("/").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per hour.")
    }

    @Test
    fun `both path and HTTP method must match for rate limiting to kick in`() = TestUtil.test(testApp) { _, http ->
        repeat(50) { http.get("/") }
        assertThat(http.get("/").status).isEqualTo(TOO_MANY_REQUESTS.status)
        assertThat(http.get("/test").status).isNotEqualTo(TOO_MANY_REQUESTS.status)
        assertThat(http.post("/").asString().status).isNotEqualTo(TOO_MANY_REQUESTS.status)
    }

    @Test
    fun `rate limit on dynamic path-params limits per endpoint, not per URL`() = TestUtil.test(testApp) { app, http ->
        repeat(50) { http.get("/dynamic/1") }
        repeat(50) { http.get("/dynamic/2") }
        repeat(50) { http.get("/dynamic/3") }
        assertThat(http.get("/dynamic/4").status).isEqualTo(TOO_MANY_REQUESTS.status)
        assertThat(http.get("/dynamic/5").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per minute.")
    }

    @Test
    fun `millisecond rate-limiting works`() = TestUtil.test(testApp) { app, http ->
        repeat(3) {
            Thread.sleep(10)
            assertThat(http.get("/ms").status).isEqualTo(OK.status)
        }
    }

}
