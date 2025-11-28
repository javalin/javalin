/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.template.JavalinPebble
import io.javalin.testtools.JavalinTest
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestPebble {

    @Test
    fun `pebble templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinPebble())
        config.routes.get("/hello1") { it.render("templates/pebble/test.peb", mapOf("message" to "Hello Pebble!")) }
    }) { app, http ->
        assertThat(http.get("/hello1").body?.string()).isEqualTo("<h1>Hello Pebble!</h1>")
    }

    @Test
    fun `pebble empty context map work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinPebble())
        config.routes.get("/hello2") { it.render("templates/pebble/test-empty-context-map.peb") }
    }) { app, http ->
        assertThat(http.get("/hello2")).isNotEqualTo("Internal server error")
    }

    @Test
    fun `pebble custom engines work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(
            JavalinPebble(
                PebbleEngine.Builder()
                    .loader(ClasspathLoader())
                    .strictVariables(true)
                    .build()
            )
        )
        config.routes.get("/hello") { it.render("templates/pebble/test.peb") }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("Server Error")
    }
}

