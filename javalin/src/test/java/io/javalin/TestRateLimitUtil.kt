/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.TOO_MANY_REQUESTS
import io.javalin.http.util.NaiveRateLimit
import io.javalin.http.util.RateLimitUtil
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TestRateLimitUtil {

    @Test
    fun `rate limiting kicks in when request limit is exceeded`() = TestUtil.test(
        Javalin.create().get("/test") { NaiveRateLimit.requestPerTimeUnit(it, 3, TimeUnit.HOURS) }
    ) { _, http ->
        repeat(3) { assertThat(http.get("/test").httpCode()).isEqualTo(OK) } // exhaust rate limit
        val response = http.get("/test") // next request should be rate-limited
        assertThat(response.httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(response.body).isEqualTo("Rate limit exceeded - Server allows 3 requests per hour.")
    }

    @Test
    fun `rate limiting is scoped to specific path and method combination`() = TestUtil.test(
        Javalin.create()
            .get("/api") { NaiveRateLimit.requestPerTimeUnit(it, 2, TimeUnit.HOURS) }
            .post("/api") { it.result("post ok") }
            .get("/other") { it.result("other ok") }
    ) { _, http ->
        repeat(2) { assertThat(http.get("/api").httpCode()).isEqualTo(OK) } // exhaust rate limit
        assertThat(http.get("/api").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.post("/api").asString().httpCode()).isEqualTo(OK) // POST should not be rate-limited
        assertThat(http.get("/other").httpCode()).isEqualTo(OK) // neither should GET /other
    }

    @Test
    fun `rate limiting works for dynamic paths`() = TestUtil.test(
        Javalin.create().get("/users/{id}") { NaiveRateLimit.requestPerTimeUnit(it, 2, TimeUnit.HOURS) }
    ) { _, http ->
        assertThat(http.get("/users/123").httpCode()).isEqualTo(OK)
        assertThat(http.get("/users/456").httpCode()).isEqualTo(OK)
        assertThat(http.get("/users/789").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
    }

    @Test
    fun `rate limiter is cleared correctly`() = TestUtil.test(
        Javalin.create().get("/") { NaiveRateLimit.requestPerTimeUnit(it, 1, TimeUnit.MILLISECONDS) }
    ) { _, http ->
        val responseCodes = mutableListOf<Int>()
        repeat(5) { responseCodes.add(http.get("/").httpCode().code) }
        assertThat(responseCodes).contains(429) // some requests should be rate-limited
        Thread.sleep(50) // wait for rate limiter to clear, factor in garbage collection pauses etc
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
    }

}
