/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.testing.TestUtil
import io.javalin.testing.TestUtil.runAndCaptureLogs
import io.javalin.testing.get
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestRendering {

    private fun renderingJavalin() = Javalin.create {
        it.fileRenderer { filePath, model, ctx ->
            "path:$filePath, model:$model, queryParam:${ctx.queryParam("q")}"
        }
    }

    @Test
    fun `FileRenderer - default renderer throws - exception`() = TestUtil.test { app, http ->
        app.get("/") { it.render("abc.ext") }
        val response = http.get("/")
        assertThat(response.status).isEqualTo(500)
        assertThat(response.body).contains("Server Error")
    }

    @Test
    fun `FileRenderer - default renderer throws - logs`() = TestUtil.test { app, http ->
        app.get("/") { it.render("abc.ext") }
        val runResult = runAndCaptureLogs { http.getBody("/") }
        assertThat(runResult.logs).contains("No FileRenderer configured. You can configure one in config.fileRenderer(...)");
    }

    @Test
    fun `FileRenderer - custom renderer works`() = TestUtil.test(renderingJavalin()) { app, http ->
        app.get("/") { it.render("/foo.myExt") }
        assertThat(http.getBody("/")).contains("queryParam:null")
    }

    @Test
    fun `FileRenderer - filepath is passed to renderer`() = TestUtil.test(renderingJavalin()) { app, http ->
        app.get("/") { it.render("/foo.myExt") }
        assertThat(http.getBody("/")).contains("path:/foo.myExt")
    }

    @Test
    fun `FileRenderer - context is passed to renderer`() = TestUtil.test(renderingJavalin()) { app, http ->
        app.get("/") { it.render("/foo.myExt") }
        assertThat(http.getBody("/?q=bar")).contains("queryParam:bar")
    }

    @Test
    fun `FileRenderer - model is passed to renderer`() = TestUtil.test(renderingJavalin()) { app, http ->
        app.get("/") { it.render("/foo.myExt", mapOf("a" to "b")) }
        assertThat(http.getBody("/")).contains("model:{a=b}")
    }

}
