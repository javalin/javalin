/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.util.TestUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TestCors {

    @Test(expected = IllegalArgumentException::class)
    fun `enableCorsForOrigin() throws for empty varargs`() {
        Javalin.create().enableCorsForOrigin()
    }

    @Test
    fun `enableCorsForOrigin() enables cors for specific origins`() = TestUtil.test(Javalin.create().enableCorsForOrigin("origin-1", "referer-1")) { app, http ->
        app.get("/") { ctx -> ctx.result("Hello") }
        val path = "http://localhost:" + app.port() + "/"
        assertThat<List<String>>(Unirest.get(path).asString().headers["Access-Control-Allow-Origin"], `is`(nullValue()))
        assertThat(Unirest.get(path).header("Origin", "origin-1").asString().headers["Access-Control-Allow-Origin"]!![0], `is`("origin-1"))
        assertThat(Unirest.get(path).header("Referer", "referer-1").asString().headers["Access-Control-Allow-Origin"]!![0], `is`("referer-1"))
    }

    @Test
    fun `enableCorsForAllOrigins() enables cors for all origins`() = TestUtil.test(Javalin.create().enableCorsForAllOrigins()) { app, http ->
        app.get("/") { ctx -> ctx.result("Hello") }
        val path = "http://localhost:" + app.port() + "/"
        assertThat(Unirest.get(path).header("Origin", "some-origin").asString().headers["Access-Control-Allow-Origin"]!![0], `is`("some-origin"))
        assertThat(Unirest.get(path).header("Referer", "some-referer").asString().headers["Access-Control-Allow-Origin"]!![0], `is`("some-referer"))
        assertThat<List<String>>(Unirest.get(path).asString().headers["Access-Control-Allow-Origin"], `is`(nullValue()))
    }

}
