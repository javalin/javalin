/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.template.JavalinThymeleaf
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestThymeleaf {

    @Test
    fun `thymeleaf templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinThymeleaf())
        config.routes.get("/hello") { it.render("/templates/thymeleaf/test.html", mapOf("message" to "Hello Thymeleaf!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Thymeleaf!</h1>")
    }

    @Test
    fun `thymeleaf url syntax work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinThymeleaf())
        config.routes.get("/hello") { it.render("/templates/thymeleaf/testUrls.html", mapOf("linkParam2" to "val2")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<a href=\"/test-link?param1=val1&amp;param2=val2\">Link text</a>")
    }
}

