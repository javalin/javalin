/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestEndpoint {

    @Test
    fun `endpoint returns the path of the endpoint used to match the request`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/matched") { it.result(it.endpoint().path) }
        app.unsafe.routes.get("/matched/{path-param}") { it.result(it.endpoint().path) }
        app.unsafe.routes.after("/matched/{path-param}/{param2}") { it.result(it.endpoint().path) }
        assertThat(http.getBody("/matched")).isEqualTo("/matched")
        assertThat(http.getBody("/matched/p1")).isEqualTo("/matched/{path-param}")
        assertThat(http.getBody("/matched/p1/p2")).isEqualTo("/matched/{path-param}/{param2}")
    }

    @Test
    fun `endpoint is available in before handler with wildcard path`() = TestUtil.test { app, http ->
        app.unsafe.routes.before { it.result(it.endpoints().current().path) }
        app.unsafe.routes.get("/endpoint") { } // NOOP
        assertThat(http.getBody("/endpoint")).isEqualTo("*")
    }

    @Test
    fun `endpoint returns correct handler type`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/endpoint") { it.result(it.endpoint().method.name()) }
        app.unsafe.routes.post("/endpoint") { it.result(it.endpoint().method.name()) }
        assertThat(http.getBody("/endpoint")).isEqualTo("GET")
        assertThat(http.post("/endpoint").asString().body).isEqualTo("POST")
    }

    @Test
    fun `lastHttpEndpoint returns the endpoint used to match the request in AFTER handlers`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/matched/{path-param}") { }
        app.unsafe.routes.after { it.result(it.endpoints().lastHttpEndpoint()?.path ?: "") }
        assertThat(http.getBody("/matched/p1")).isEqualTo("/matched/{path-param}")
    }

    @Test
    fun `endpoints stack contains all visited endpoints in order`() = TestUtil.test { app, http ->
        app.unsafe.routes.before { it.result("${it.endpoints().list().size}") }
        app.unsafe.routes.get("/test") { it.result(it.result() + "-${it.endpoints().list().size}") }
        app.unsafe.routes.after { it.result(it.result() + "-${it.endpoints().list().size}") }
        assertThat(http.getBody("/test")).isEqualTo("1-2-3")
    }

    @Test
    fun `endpoints stack is available in all handler types`() = TestUtil.test { app, http ->
        val results = mutableListOf<String>()
        app.unsafe.routes.before { results.add("before:${it.endpoints().list().size}") }
        app.unsafe.routes.get("/test") { results.add("http:${it.endpoints().list().size}") }
        app.unsafe.routes.after { results.add("after:${it.endpoints().list().size}") }
        http.getBody("/test")
        assertThat(results).containsExactly("before:1", "http:2", "after:3")
    }

}
