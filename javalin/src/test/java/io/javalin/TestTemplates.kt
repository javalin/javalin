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
import io.javalin.jte.PrecompileJteTestClasses
import io.javalin.plugin.rendering.JavalinRenderer
import io.javalin.plugin.rendering.template.JavalinJte
import io.javalin.plugin.rendering.template.JavalinPebble
import io.javalin.plugin.rendering.template.JavalinVelocity
import io.javalin.plugin.rendering.template.TemplateUtil.model
import io.javalin.testing.TestUtil
import org.apache.velocity.app.VelocityEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestTemplates {

    private val defaultBaseModel = model("foo", "bar")

    @Test
    fun `velocity templates work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(JavalinVelocity.defaultVelocityEngine())
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test.vm", model("message", "Hello Velocity!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello Velocity!</h1>")
    }

    @Test
    fun `velocity template variables work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(JavalinVelocity.defaultVelocityEngine())
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test-set.vm") }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Set works</h1>")
    }

    @Test
    fun `velocity external templates work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(VelocityEngine().apply {
            setProperty("file.resource.loader.path", "src/test/resources/templates/velocity")
        })
        app.get("/hello") { ctx -> ctx.render("test.vm") }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>\$message</h1>")
    }

    @Test
    fun `freemarker templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/freemarker/test.ftl", model("message", "Hello Freemarker!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello Freemarker!</h1>")
    }

    @Test
    fun `thymeleaf templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/thymeleaf/test.html", model("message", "Hello Thymeleaf!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello Thymeleaf!</h1>")
    }

    @Test
    fun `thymeleaf url syntax work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/thymeleaf/testUrls.html", model("linkParam2", "val2")) }
        assertThat(http.getBody("/hello")).isEqualTo("<a href=\"/test-link?param1=val1&amp;param2=val2\">Link text</a>")
    }

    @Test
    fun `mustache templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/mustache/test.mustache", model("message", "Hello Mustache!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello Mustache!</h1>")
    }

    @Test
    fun `pebble templates work`() = TestUtil.test { app, http ->
        app.get("/hello1") { ctx -> ctx.render("templates/pebble/test.peb", model("message", "Hello Pebble!")) }
        assertThat(http.getBody("/hello1")).isEqualTo("<h1>Hello Pebble!</h1>")
    }

    @Test
    fun `pebble empty context map work`() = TestUtil.test { app, http ->
        app.get("/hello2") { ctx -> ctx.render("templates/pebble/test-empty-context-map.peb") }
        assertThat(http.getBody("/hello2")).isNotEqualTo("Internal server error")
    }

    @Test
    fun `pebble custom engines work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("templates/pebble/test.peb") }
        assertThat(http.getBody("/hello")).isEqualTo("<h1></h1>")
        JavalinPebble.configure(
            PebbleEngine.Builder()
                .loader(ClasspathLoader())
                .strictVariables(true)
                .build()
        )
        assertThat(http.getBody("/hello")).isEqualTo("Internal server error")
    }

    @Test
    fun `jte works`() = TestUtil.test { app, http ->
        JavalinJte.configure(TemplateEngine.createPrecompiled(null, ContentType.Html, null, PrecompileJteTestClasses.PACKAGE_NAME))
        app.get("/hello") { ctx -> ctx.render("test.jte", model("page", JteTestPage("hello", "world"))) }
        assertThat(http.getBody("/hello")).isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

    @Test
    fun `jte multiple params work`() = TestUtil.test { app, http ->
        JavalinJte.configure(TemplateEngine.createPrecompiled(null, ContentType.Html, null, PrecompileJteTestClasses.PACKAGE_NAME))
        app.get("/hello") { ctx -> ctx.render("multiple-params.jte", model("one", "hello", "two", "world")) }
        assertThat(http.getBody("/hello")).isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

    @Test
    fun `jte kotlin works`() = TestUtil.test { app, http ->
        JavalinJte.configure(TemplateEngine.createPrecompiled(null, ContentType.Html, null, PrecompileJteTestClasses.PACKAGE_NAME))
        app.get("/hello") { ctx -> ctx.render("kte/test.kte", model("page", JteTestPage("hello", "world"))) }
        assertThat(http.getBody("/hello")).isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

    @Test
    fun `jte kotlin multiple params work`() = TestUtil.test { app, http ->
        JavalinJte.configure(TemplateEngine.createPrecompiled(null, ContentType.Html, null, PrecompileJteTestClasses.PACKAGE_NAME))
        app.get("/hello") { ctx -> ctx.render("kte/multiple-params.kte", model("one", "hello", "two", "world")) }
        assertThat(http.getBody("/hello")).isEqualToIgnoringNewLines("<h1>hello world!</h1>")
    }

    @Test
    fun `markdown works`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/markdown/test.md") }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello Markdown!</h1>\n")
    }

    @Test
    fun `unregistered extension throws exception`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/markdown/test.unknown") }
        assertThat(http.getBody("/hello")).isEqualTo("Internal server error")
    }

    @Test
    fun `registering custom renderer works`() = TestUtil.test { app, http ->
        JavalinRenderer.register({ _, _, _ -> "Hah." }, ".lol")
        app.get("/hello") { ctx -> ctx.render("/markdown/test.lol") }
        assertThat(http.getBody("/hello")).isEqualTo("Hah.")
    }

    @Test
    fun `base Model works`() = TestUtil.test { app, http ->
        JavalinRenderer.baseModelFunction = { ctx -> defaultBaseModel + mapOf("queryParams" to ctx.queryParamMap(), "pathParams" to ctx.pathParamMap()) }
        app.get("/hello/{pp}") { ctx -> ctx.render("/templates/freemarker/test-with-base.ftl", model("message", "Hello Freemarker!")) }
        assertThat(http.getBody("/hello/world?im=good")).isEqualTo("<h1>good</h1><h2>world</h2><h3>bar</h3>")
    }

    @Test
    fun `base model overwrite works`() = TestUtil.test { app, http ->
        JavalinRenderer.baseModelFunction = { ctx -> defaultBaseModel + mapOf("queryParams" to ctx.queryParamMap(), "pathParams" to ctx.pathParamMap()) }
        app.get("/hello/{pp}") { ctx -> ctx.render("/templates/freemarker/test-with-base.ftl", model("foo", "baz")) }
        assertThat(http.getBody("/hello/world?im=good")).isEqualTo("<h1>good</h1><h2>world</h2><h3>baz</h3>")
    }

    data class JteTestPage(val hello: String, val world: String)
}
