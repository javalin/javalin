/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.rendering.JavalinRenderer
import io.javalin.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class TestRendering {

    @Test
    fun `custom renderer works`() = TestUtil.test { app, http ->
        app.get("/") { it.render("/foo.myExt") }
        assertThat(http.getBody("/")).contains("queryParam:null")
    }

    @Test
    fun `filepath is passed to renderer`() = TestUtil.test { app, http ->
        app.get("/") { it.render("/foo.myExt") }
        assertThat(http.getBody("/")).contains("path:/foo.myExt")
    }

    @Test
    fun `context is passed to renderer`() = TestUtil.test { app, http ->
        app.get("/") { it.render("/foo.myExt") }
        assertThat(http.getBody("/?q=bar")).contains("queryParam:bar")
    }

    @Test
    fun `model is passed to renderer`() = TestUtil.test { app, http ->
        app.get("/") { it.render("/foo.myExt", mapOf("a" to "b")) }
        assertThat(http.getBody("/")).contains("model:{a=b}")
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            JavalinRenderer.register({ filePath, model, context ->
                "path:$filePath, model:$model, queryParam:${context.queryParam("q")}"
            }, ".myExt")
        }
    }

}
