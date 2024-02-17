/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import gg.jte.ContentType
import gg.jte.TemplateEngine
import io.javalin.jte.JteTestPage
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.markdown.JavalinCommonmark
import io.javalin.rendering.template.*
import io.javalin.testtools.JavalinTest
import org.apache.velocity.app.VelocityEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestTemplates {

    private fun app(fileRenderer: FileRenderer) = Javalin.create { it.fileRenderer(fileRenderer) }

    @Test
    fun `velocity templates work`() = JavalinTest.test(app(JavalinVelocity())) { app, http ->
        app.get("/hello") { it.render("/templates/velocity/test.vm", mapOf("message" to "Hello Velocity!")) }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Velocity!</h1>")
    }

    @Test
    fun `velocity template variables work`() = JavalinTest.test(app(JavalinVelocity())) { app, http ->
        app.get("/hello") { it.render("/templates/velocity/test-set.vm") }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Set works</h1>")
    }

    @Test
    fun `velocity external templates work`() = JavalinTest.test(app(JavalinVelocity(VelocityEngine().apply {
        setProperty("resource.loader.file.path", "src/test/resources/templates/velocity")
    }))) { app, http ->
        app.get("/hello") { it.render("test.vm") }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>\$message</h1>")
    }

    @Test
    fun `freemarker templates work`() = JavalinTest.test(app(JavalinFreemarker())) { app, http ->
        app.get("/hello") { it.render("/templates/freemarker/test.ftl", mapOf("message" to "Hello Freemarker!")) }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Freemarker!</h1>")
    }

    fun `thymeleaf templates work`() = JavalinTest.test(app(JavalinThymeleaf())) { app, http ->
        app.get("/hello") { it.render("/templates/thymeleaf/test.html", mapOf("message" to "Hello Thymeleaf!")) }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Thymeleaf!</h1>")
    }

    @Test
    fun `thymeleaf url syntax work`() = JavalinTest.test(app(JavalinThymeleaf())) { app, http ->
        app.get("/hello") { it.render("/templates/thymeleaf/testUrls.html", mapOf("linkParam2" to "val2")) }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<a href=\"/test-link?param1=val1&amp;param2=val2\">Link text</a>")
    }

    @Test
    fun `mustache templates work`() = JavalinTest.test(app(JavalinMustache())) { app, http ->
        app.get("/hello") { it.render("/templates/mustache/test.mustache", mapOf("message" to "Hello Mustache!")) }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Mustache!</h1>")
    }

    @Test
    fun `pebble templates work`() = JavalinTest.test(app(JavalinPebble())) { app, http ->
        app.get("/hello1") { it.render("templates/pebble/test.peb", mapOf("message" to "Hello Pebble!")) }
        assertThat(http.get("/hello1").body?.string()).isEqualTo("<h1>Hello Pebble!</h1>")
    }

    @Test
    fun `pebble empty context map work`() = JavalinTest.test(app(JavalinPebble())) { app, http ->
        app.get("/hello2") { it.render("templates/pebble/test-empty-context-map.peb") }
        assertThat(http.get("/hello2")).isNotEqualTo("Internal server error")
    }

    @Test
    fun `pebble custom engines work`() = JavalinTest.test(
        app(
            JavalinPebble(
                PebbleEngine.Builder()
                    .loader(ClasspathLoader())
                    .strictVariables(true)
                    .build()
            )
        )
    ) { app, http ->
        app.get("/hello") { it.render("templates/pebble/test.peb") }
        assertThat(http.get("/hello").body?.string()).isEqualTo("Server Error")
    }

    @Test
    fun `markdown works`() = JavalinTest.test(app(JavalinCommonmark())) { app, http ->
        app.get("/hello") { it.render("/markdown/test.md") }
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello Markdown!</h1>\n")
    }

}
