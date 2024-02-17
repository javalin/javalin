package io.javalin.jte

import gg.jte.ContentType
import gg.jte.TemplateEngine
import io.javalin.Javalin
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.template.JavalinJte
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class TestJte {

    private fun app(fileRenderer: FileRenderer) = Javalin.create { it.fileRenderer(fileRenderer) }

    private fun javalinJte() = JavalinJte(
        TemplateEngine.createPrecompiled(
            null,
            ContentType.Html,
            null,
            "io.javalin.jte.precompiled"
        )
    )

    val errorMessage = "if this fails, you need to run mvn gg.jte:jte-maven-plugin:generate first"

    @Test
    fun `jte works`() = JavalinTest.test(app(javalinJte())) { app, http ->
        app.get("/hello") { it.render("test.jte", mapOf("page" to JteTestPage("hello", "world"))) }
        assertThat(http.get("/hello").body?.string())
            .describedAs(errorMessage)
            .isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

    @Test
    fun `jte multiple params work`() = JavalinTest.test(app(javalinJte())) { app, http ->
        app.get("/hello") { it.render("multiple-params.jte", mapOf("one" to "hello", "two" to "world")) }
        assertThat(http.get("/hello").body?.string())
            .describedAs(errorMessage)
            .isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

    @Test
    fun `jte kotlin works`() = JavalinTest.test(app(javalinJte())) { app, http ->
        app.get("/hello") { it.render("kte/test.kte", mapOf("page" to JteTestPage("hello", "world"))) }
        assertThat(http.get("/hello").body?.string())
            .describedAs(errorMessage)
            .isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

    @Test
    fun `jte kotlin multiple params work`() = JavalinTest.test(app(javalinJte())) { app, http ->
        app.get("/hello") { it.render("kte/multiple-params.kte", mapOf("one" to "hello", "two" to "world")) }
        assertThat(http.get("/hello").body?.string())
            .describedAs(errorMessage)
            .isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

}
