/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.template.JavalinHandlebars
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHandlebars {

    @Test
    fun `handlebars templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinHandlebars())
        config.routes.get("/hello") { it.render("/templates/handlebars/test.hbs", mapOf("message" to "Hello Handlebars!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()?.trim()).isEqualTo("<h1>Hello Handlebars!</h1>")
    }
}
