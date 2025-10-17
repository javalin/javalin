/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.template.JavalinVelocity
import io.javalin.testtools.JavalinTest
import org.apache.velocity.app.VelocityEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestVelocity {

    @Test
    fun `velocity templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinVelocity())
        config.routes.get("/hello") { it.render("/templates/velocity/test.vm", mapOf("message" to "Hello Velocity!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Velocity!</h1>")
    }

    @Test
    fun `velocity template variables work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinVelocity())
        config.routes.get("/hello") { it.render("/templates/velocity/test-set.vm") }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Set works</h1>")
    }

    @Test
    fun `velocity external templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinVelocity(VelocityEngine().apply {
            setProperty("resource.loader.file.path", "src/test/resources/templates/velocity")
        }))
        config.routes.get("/hello") { it.render("test.vm") }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>\$message</h1>")
    }
}

