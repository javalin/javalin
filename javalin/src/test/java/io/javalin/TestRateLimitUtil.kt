/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.TOO_MANY_REQUESTS
import io.javalin.http.util.NaiveRateLimit
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
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
        repeat(20) { http.get("/") }
        assertThat(http.get("/").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.get("/").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per hour.")
    }

    @Test
    fun `both path and HTTP method must match for rate limiting to kick in`() = TestUtil.test(testApp) { _, http ->
        repeat(20) { http.get("/") }
        assertThat(http.get("/").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.get("/test").httpCode()).isNotEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.post("/").asString().httpCode()).isNotEqualTo(TOO_MANY_REQUESTS)
    }

    @Test
    fun `rate limit on dynamic path-params limits per endpoint, not per URL`() = TestUtil.test(testApp) { app, http ->
        repeat(20) { http.get("/dynamic/1") }
        repeat(20) { http.get("/dynamic/2") }
        repeat(20) { http.get("/dynamic/3") }
        assertThat(http.get("/dynamic/4").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.get("/dynamic/5").body).isEqualTo("Rate limit exceeded - Server allows 5 requests per minute.")
    }

    @Test
    fun `clearing the rate-limiter works`() = TestUtil.test(testApp) { app, http ->
        assertThat(http.get("/ms").httpCode()).isEqualTo(OK)
        Thread.sleep(50) // surely this is enough time
        assertThat(http.get("/ms").httpCode()).isEqualTo(OK)
    }

}
