/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.http.util.NaiveRateLimit
import io.javalin.testing.TestUtil
import io.javalin.testing.*
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TestRateLimitUtil {

    @Test
    fun `rate limiting kicks in when request limit is exceeded`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/") { NaiveRateLimit.requestPerTimeUnit(it, 3, TimeUnit.HOURS) }
        }
    ) { _, http ->
        val responseCodes = mutableListOf<Int>()
        repeat(6) { responseCodes.add(http.get("/").httpCode().code) }
        assertThat(responseCodes).containsAll(listOf(200, 429))
    }

    @Test
    fun `rate limiting is scoped to specific path and method combination`() = TestUtil.test(
        Javalin.create()
            .get("/api") { NaiveRateLimit.requestPerTimeUnit(it, 2, TimeUnit.HOURS) }
            .post("/api") { it.result("post ok") }
            .get("/other") { it.result("other ok") }
    ) { _, http ->
        val responseCodes = mutableListOf<Int>()
        repeat(6) { responseCodes.add(http.get("/api").httpCode().code) }
        assertThat(responseCodes).containsAll(listOf(200, 429))
        assertThat(http.post("/api").asString().httpCode()).isEqualTo(OK) // POST should not be rate-limited
        assertThat(http.get("/other").httpCode()).isEqualTo(OK) // neither should GET /other
    }

    @Test
    fun `rate limiting works for dynamic paths`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/users/{id}") { NaiveRateLimit.requestPerTimeUnit(it, 2, TimeUnit.HOURS) }
        }
    ) { _, http ->
        val responseCodes = mutableListOf<Int>()
        for (i in 1..5) {
            responseCodes.add(http.get("/users/$i").httpCode().code)
        }
        assertThat(responseCodes).containsAll(listOf(200, 429)) // some requests should be rate-limited
    }

    @Test
    fun `rate limiter is cleared correctly`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/") { NaiveRateLimit.requestPerTimeUnit(it, 1, TimeUnit.MILLISECONDS) }
        }
    ) { _, http ->
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
        Thread.sleep(50) // wait for rate limiter to clear, factor in garbage collection pauses etc
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
    }

}
