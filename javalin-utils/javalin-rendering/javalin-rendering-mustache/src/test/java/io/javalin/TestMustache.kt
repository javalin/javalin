/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.template.JavalinMustache
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestMustache {

    @Test
    fun `mustache templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinMustache())
        config.routes.get("/hello") { it.render("/templates/mustache/test.mustache", mapOf("message" to "Hello Mustache!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Mustache!</h1>")
    }
}

