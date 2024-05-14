/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TestBodyReading {

    @Test
    fun `reading body as bytes works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.bodyAsBytes()) }
        assertThat(http.post("/").body("body").asString().body).isEqualTo("body")
    }

    @Test
    fun `reading query-params then body works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.body() + "|" + it.queryParam("qp")) }
        val response = http.post("/").queryString("qp", "param").body("body").asString()
        assertThat(response.body).isEqualTo("body|param")
    }

    @Test
    fun `reading body then query-params works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.queryParam("qp") + "|" + it.body()) }
        val response = http.post("/").queryString("qp", "param").body("body").asString()
        assertThat(response.body).isEqualTo("param|body")
    }

    @Test
    fun `reading form-params then body works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.formParam("fp") + "|" + it.body()) }
        val response = http.post("/").body("fp=param").asString()
        assertThat(response.body).isEqualTo("param|fp=param")
    }

    @Test
    fun `reading body then form-params works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.body() + "|" + it.formParam("fp")) }
        val response = http.post("/").body("fp=param").asString()
        assertThat(response.body).isEqualTo("fp=param|param")
    }

    @Test
    fun `reading unicode form-params works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.formParam("fp")!!) }
        val response = http.post("/").body("fp=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟").asString()
        assertThat(response.body).isEqualTo("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
    }

    @Test
    fun `reading invalid form-params without contentType works`() = TestUtil.test { app, http ->
        app.post("/") { it.result((it.formParam("fp") == null).toString()) }
        val response = http.post("/").body("fp=%+").asString()
        assertThat(response.body).isEqualTo("true")
    }

    @Test // not sure why this does so much...
    fun `query-params and form-params behave the same`() = TestUtil.test { app, http ->
        app.post("/") { ctx ->
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
        val response = http.post("/?$params").body(params).asString()
        assertThat(response.body).isEqualTo("a: 1, as: [1]. b: 1, bs: [1, 2, 3]. c: 1=1, cs: [1=1]. d: , ds: []. e: , es: []. f: ( # ), fs: [( # )]. <g>: g, <g>s: [g]")
    }

    @Test
    fun `reading body as stream works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.bodyInputStream()) }
        assertThat(http.post("/").body("body").asString().body).isEqualTo("body")
    }

    @Test
    fun `reading body multiple times works`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.body() + it.body()) }
        assertThat(http.post("/").body("body").asString().body).isEqualTo("bodybody")
    }

    @Test
    fun `reading too large request body does not work`() {
        val result = TestUtil.testWithResult(Javalin.create { it.http.maxRequestSize = 1000 }) { app, http ->
            app.post("/") { it.result(it.body() + it.body()) }
            http.post("/").body("x".repeat(1001)).asString()
        }
        assertThat(result.logs).contains("Body greater than max size (1000 bytes)")
    }

    private fun urlEncode(text: String) = URLEncoder.encode(text, StandardCharsets.UTF_8.name())

}
