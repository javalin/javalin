/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.rendering.FileRenderer
import io.javalin.rendering.markdown.JavalinCommonmark
import io.javalin.rendering.template.*
import io.javalin.testtools.JavalinTest
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.apache.velocity.app.VelocityEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestTemplates {

    private fun app(fileRenderer: FileRenderer) = Javalin.create { it.fileRenderer(fileRenderer) }

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

    @Test
    fun `freemarker templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinFreemarker())
        config.routes.get("/hello") { it.render("/templates/freemarker/test.ftl", mapOf("message" to "Hello Freemarker!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Freemarker!</h1>")
    }

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

    @Test
    fun `mustache templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinMustache())
        config.routes.get("/hello") { it.render("/templates/mustache/test.mustache", mapOf("message" to "Hello Mustache!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Mustache!</h1>")
    }

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

    @Test
    fun `markdown works`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinCommonmark())
        config.routes.get("/hello") { it.render("/markdown/test.md") }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Markdown!</h1>\n")
    }

}
