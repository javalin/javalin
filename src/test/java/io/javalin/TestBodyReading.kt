/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestBodyReading {

    @Test
    fun test_bodyReader() = TestUtil().test { app, http ->
        app.before("/body-reader") { ctx -> ctx.header("X-BEFORE", ctx.body() + ctx.queryParam("qp")!!) }
        app.post("/body-reader") { ctx -> ctx.result(ctx.body() + ctx.queryParam("qp")!!) }
        app.after("/body-reader") { ctx -> ctx.header("X-AFTER", ctx.body() + ctx.queryParam("qp")!!) }
        val response = http.post("/body-reader")
                .queryString("qp", "queryparam")
                .body("body")
                .asString()
        assertThat(response.headers.getFirst("X-BEFORE"), `is`("bodyqueryparam"))
        assertThat(response.body, `is`("bodyqueryparam"))
        assertThat(response.headers.getFirst("X-AFTER"), `is`("bodyqueryparam"))
    }

    @Test
    fun test_bodyReader_reverse() = TestUtil().test { app, http ->
        app.before("/body-reader") { ctx -> ctx.header("X-BEFORE", ctx.queryParam("qp")!! + ctx.body()) }
        app.post("/body-reader") { ctx -> ctx.result(ctx.queryParam("qp")!! + ctx.body()) }
        app.after("/body-reader") { ctx -> ctx.header("X-AFTER", ctx.queryParam("qp")!! + ctx.body()) }
        val response = http.post("/body-reader")
                .queryString("qp", "queryparam")
                .body("body")
                .asString()
        assertThat(response.headers.getFirst("X-BEFORE"), `is`("queryparambody"))
        assertThat(response.body, `is`("queryparambody"))
        assertThat(response.headers.getFirst("X-AFTER"), `is`("queryparambody"))
    }

    @Test
    fun test_formParams_work() = TestUtil().test { app, http ->
        app.before("/body-reader") { ctx -> ctx.header("X-BEFORE", ctx.formParam("username")!!) }
        app.post("/body-reader") { ctx -> ctx.result(ctx.formParam("password")!!) }
        app.after("/body-reader") { ctx -> ctx.header("X-AFTER", ctx.formParam("repeat-password")!!) }
        val response = http.post("/body-reader")
                .body("username=some-user-name&password=password&repeat-password=password")
                .asString()
        assertThat(response.headers.getFirst("X-BEFORE"), `is`("some-user-name"))
        assertThat(response.body, `is`("password"))
        assertThat(response.headers.getFirst("X-AFTER"), `is`("password"))
    }

    @Test
    fun test_unicodeFormParams_work() = TestUtil().test { app, http ->
        app.post("/unicode") { ctx -> ctx.result(ctx.formParam("unicode")!!) }
        val responseBody = http.post("/unicode")
                .body("unicode=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
                .asString().body
        assertThat(responseBody, `is`("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"))
    }

    @Test // not sure why this does so much...
    fun test_formParamsWork_multipleValues() = TestUtil().test { app, http ->
        app.post("/body-reader") { ctx ->
            val formParamString = ctx.formParamMap().map { it.key + ": " + ctx.formParam(it.key) + ", " + it.key + "s: " + ctx.formParams(it.key).toString() }.joinToString(". ")
            val queryParamString = ctx.queryParamMap().map { it.key + ": " + ctx.queryParam(it.key) + ", " + it.key + "s: " + ctx.queryParams(it.key).toString() }.joinToString(". ")
            val singleMissingSame = ctx.formParam("missing") == ctx.queryParam("missing")
            val pluralMissingSame = ctx.formParams("missing") == ctx.queryParams("missing")
            val nonMissingSame = formParamString == queryParamString
            if (singleMissingSame && pluralMissingSame && nonMissingSame) {
                ctx.result(formParamString)
            }
        }
        val params = "a=1&a=2&a=3&b=1&b=2&c=1&d=&e&f=%28%23%29"
        val response = http.post("/body-reader?$params").body(params).asString()
        assertThat(response.body, `is`("a: 1, as: [1, 2, 3]. b: 1, bs: [1, 2]. c: 1, cs: [1]. d: , ds: []. e: , es: []. f: (#), fs: [(#)]"))
    }

}
