/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.template.JavalinFreemarker
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestFreemarker {

    @Test
    fun `freemarker templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinFreemarker())
        config.routes.get("/hello") { it.render("/templates/freemarker/test.ftl", mapOf("message" to "Hello Freemarker!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Freemarker!</h1>")
    }
}

