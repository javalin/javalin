/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TestBodyReading {

    @Test
    fun `reading body as bytes works`() = TestUtil.test { app, http ->
        app.post("/body-reader") { ctx -> ctx.result(ByteArrayInputStream(ctx.bodyAsBytes())) }
        assertThat(http.post("/body-reader").body("my-body").asString().body).isEqualTo("my-body")
    }

    @Test
    fun `reading query-params then body works`() = TestUtil.test { app, http ->
        app.post("/body-reader") { ctx -> ctx.result(ctx.body() + "|" + ctx.queryParam("qp")!!) }
        val response = http.post("/body-reader")
                .queryString("qp", "queryparam")
                .body("body")
                .asString()
        assertThat(response.body).isEqualTo("body|queryparam")
    }

    @Test
    fun `reading body then query-params works`() = TestUtil.test { app, http ->
        app.post("/body-reader") { ctx -> ctx.result(ctx.queryParam("qp")!! + "|" + ctx.body()) }
        val response = http.post("/body-reader")
                .queryString("qp", "queryparam")
                .body("body")
                .asString()
        assertThat(response.body).isEqualTo("queryparam|body")
    }

    @Test
    fun `reading form-params then body works`() = TestUtil.test { app, http ->
        app.post("/body-reader") { ctx -> ctx.result(ctx.formParam("username")!! + "|" + ctx.body()) }
        val response = http.post("/body-reader").body("username=some-user").asString()
        assertThat(response.body).isEqualTo("some-user|username=some-user")
    }

    @Test
    fun `reading body then form-params works`() = TestUtil.test { app, http ->
        app.post("/body-reader") { ctx -> ctx.result(ctx.body() + "|" + ctx.formParam("username")!!) }
        val response = http.post("/body-reader").body("username=some-user").asString()
        assertThat(response.body).isEqualTo("username=some-user|some-user")
    }

    @Test
    fun `reading unicode form-params works`() = TestUtil.test { app, http ->
        app.post("/unicode") { ctx -> ctx.result(ctx.formParam("unicode")!!) }
        val responseBody = http.post("/unicode")
                .body("unicode=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
                .asString().body
        assertThat(responseBody).isEqualTo("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
    }

    @Test // not sure why this does so much...
    fun `query-params and form-params behave the same`() = TestUtil.test { app, http ->
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
        val params = "a=1" +      // single value
                "&b=1&b=2&b=3" +  // multiple values
                "&c=1=1" +        // value with '=' character
                "&d=" +           // empty value
                "&e" +            // also empty value
                "&f=" + urlEncode("( # )") +   // %-encoded value with special chars
                "&" + urlEncode("<g>") + "=g"  // %-encoded key with special chars
        val response = http.post("/body-reader?$params").body(params).asString()
        assertThat(response.body).isEqualTo("a: 1, as: [1]. b: 1, bs: [1, 2, 3]. c: 1=1, cs: [1=1]. d: , ds: []. e: , es: []. f: ( # ), fs: [( # )]. <g>: g, <g>s: [g]")
    }

    private fun urlEncode(text: String) = URLEncoder.encode(text, StandardCharsets.UTF_8.name())

}
