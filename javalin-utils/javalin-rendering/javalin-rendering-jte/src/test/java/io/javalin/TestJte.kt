/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.rendering.template.JavalinJte
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TestJte {

    @Test
    fun `jte templates work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinJte())
        config.routes.get("/hello") { it.render("templates/jte/test.jte", mapOf("message" to "Hello JTE!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello JTE!</h1>")
    }

    @Test
    fun `jte template variables work`() = JavalinTest.test(Javalin.create { config ->
        config.fileRenderer(JavalinJte())
        config.routes.get("/hello") { it.render("templates/jte/test-variable.jte", mapOf("name" to "World")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>Hello, World!</h1>")
    }

    @Test
    fun `jte external templates work`() = JavalinTest.test(Javalin.create { config ->
        val codeResolver = DirectoryCodeResolver(Path.of("src/test/resources/templates/jte"))
        val templateEngine = TemplateEngine.create(codeResolver, ContentType.Html)
        config.fileRenderer(JavalinJte(templateEngine))
        config.routes.get("/hello") { it.render("test.jte", mapOf("message" to "External Template!")) }
    }) { app, http ->
        assertThat(http.get("/hello").body?.string()).isEqualTo("<h1>External Template!</h1>")
    }
}


