/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.testing.TestUtil
import io.javalin.testing.TestUtil.okHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestHttpVerbs {

    @Test
    fun `basic hello world works`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello World") }
        assertThat(http.getBody("/hello")).isEqualTo("Hello World")
    }

    @Test
    fun `all mapped verbs return 200`() = TestUtil.test { app, http ->
        app.get("/mapped", okHandler)
        app.post("/mapped", okHandler)
        app.put("/mapped", okHandler)
        app.delete("/mapped", okHandler)
        app.patch("/mapped", okHandler)
        app.head("/mapped", okHandler)
        app.options("/mapped", okHandler)
        for (httpMethod in HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/mapped").status).isEqualTo(200)
        }
    }

    @Test
    fun `all unmapped verbs return 404`() = TestUtil.test { _, http ->
        for (httpMethod in HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/unmapped").status).isEqualTo(404)
        }
    }

    @Test
    fun `HEAD returns 200 if GET is mapped`() = TestUtil.test { app, http ->
        app.get("/mapped", okHandler)
        assertThat(http.call(HttpMethod.HEAD, "/mapped").status).isEqualTo(200)
    }

    @Test
    fun `filers are executed in order`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.result("1") }
        app.before { ctx -> ctx.result(ctx.resultString()!! + "2") }
        app.before { ctx -> ctx.result(ctx.resultString()!! + "3") }
        app.before { ctx -> ctx.result(ctx.resultString()!! + "4") }
        app.get("/hello") { ctx -> ctx.result(ctx.resultString()!! + "Hello") }
        app.after { ctx -> ctx.result(ctx.resultString()!! + "5") }
        assertThat(http.getBody("/hello")).isEqualTo("1234Hello5")
    }

}
