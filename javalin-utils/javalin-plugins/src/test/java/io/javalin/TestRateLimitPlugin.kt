/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.TOO_MANY_REQUESTS
import io.javalin.plugin.RateLimitPlugin
import io.javalin.testing.asString
import io.javalin.testing.get
import io.javalin.testing.getBody
import io.javalin.testing.httpCode
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TestRateLimitPlugin {

    @Test
    fun `rate limiting kicks in when request limit is exceeded`() = JavalinTest.test(
        Javalin.create { config ->
            config.registerPlugin(RateLimitPlugin { })
            config.routes.get("/") { ctx ->
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(3, TimeUnit.HOURS)
                ctx.result("OK")
            }
        }
    ) { _, http ->
        repeat(3) { assertThat(http.get("/").httpCode()).isEqualTo(OK) }
        assertThat(http.get("/").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
    }

    @Test
    fun `rate limiting is scoped to path and method`() = JavalinTest.test(
        Javalin.create { config ->
            config.registerPlugin(RateLimitPlugin { })
            config.routes.get("/api") { ctx ->
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(2, TimeUnit.HOURS)
                ctx.result("OK")
            }
            config.routes.post("/api") { ctx -> ctx.result("post ok") }
            config.routes.get("/other") { ctx -> ctx.result("other ok") }
        }
    ) { _, http ->
        repeat(2) { assertThat(http.get("/api").httpCode()).isEqualTo(OK) }
        assertThat(http.get("/api").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.post("/api").asString().httpCode()).isEqualTo(OK)
        assertThat(http.get("/other").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `rate limiting works for dynamic paths`() = JavalinTest.test(
        Javalin.create { config ->
            config.registerPlugin(RateLimitPlugin { })
            config.routes.get("/users/{id}") { ctx ->
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(2, TimeUnit.HOURS)
                ctx.result("OK")
            }
        }
    ) { _, http ->
        assertThat(http.get("/users/1").httpCode()).isEqualTo(OK)
        assertThat(http.get("/users/2").httpCode()).isEqualTo(OK)
        assertThat(http.get("/users/3").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
    }

    @Test
    fun `rate limiter is cleared by scheduled executor`() = JavalinTest.test(
        Javalin.create { config ->
            config.registerPlugin(RateLimitPlugin { })
            config.routes.get("/") { ctx ->
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(1, TimeUnit.MILLISECONDS)
                ctx.result("OK")
            }
        }
    ) { _, http ->
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
        Thread.sleep(50)
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `custom key function can be configured`() = JavalinTest.test(
        Javalin.create { config ->
            config.registerPlugin(RateLimitPlugin { cfg ->
                cfg.keyFunction = { ctx -> ctx.path() }
            })
            config.routes.get("/api") { ctx ->
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(2, TimeUnit.HOURS)
                ctx.result("OK")
            }
            config.routes.post("/api") { ctx ->
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(2, TimeUnit.HOURS)
                ctx.result("OK")
            }
        }
    ) { _, http ->
        assertThat(http.get("/api").httpCode()).isEqualTo(OK)
        assertThat(http.post("/api").asString().httpCode()).isEqualTo(OK)
        assertThat(http.get("/api").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.post("/api").asString().httpCode()).isEqualTo(TOO_MANY_REQUESTS)
    }

    @Test
    fun `getCurrentCount returns correct count`() = JavalinTest.test(
        Javalin.create { config ->
            config.registerPlugin(RateLimitPlugin { })
            config.routes.get("/count") { ctx ->
                val currentCount = ctx.with(RateLimitPlugin::class).getCurrentCount(TimeUnit.HOURS)
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(5, TimeUnit.HOURS)
                ctx.result("Count: $currentCount")
            }
        }
    ) { _, http ->
        assertThat(http.getBody("/count")).isEqualTo("Count: 0")
        assertThat(http.getBody("/count")).isEqualTo("Count: 1")
        assertThat(http.getBody("/count")).isEqualTo("Count: 2")
    }

    @Test
    fun `X-Forwarded-For header is used by default`() = JavalinTest.test(
        Javalin.create { config ->
            config.registerPlugin(RateLimitPlugin { })
            config.routes.get("/") { ctx ->
                ctx.with(RateLimitPlugin::class).requestPerTimeUnit(1, TimeUnit.HOURS)
                ctx.result("OK")
            }
        }
    ) { _, http ->
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
        assertThat(http.get("/").httpCode()).isEqualTo(TOO_MANY_REQUESTS)
        assertThat(http.get("/", mapOf("X-Forwarded-For" to "1.2.3.4")).httpCode()).isEqualTo(OK)
        assertThat(http.get("/", mapOf("X-Forwarded-For" to "5.6.7.8")).httpCode()).isEqualTo(OK)
    }
}

