/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.markdown.JavalinCommonmark
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestCommonmark {

    @Test
    fun `markdown works`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinCommonmark())
        config.routes.get("/hello") { it.render("/markdown/test.md") }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Markdown!</h1>\n")
    }
}

