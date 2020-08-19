/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.javalin.plugin.rendering.FileRenderer
import io.javalin.plugin.rendering.JavalinRenderer
import io.javalin.plugin.rendering.template.JavalinJte
import io.javalin.plugin.rendering.template.JavalinJtwig
import io.javalin.plugin.rendering.template.JavalinPebble
import io.javalin.plugin.rendering.template.JavalinVelocity
import io.javalin.plugin.rendering.template.TemplateUtil.model
import io.javalin.testing.TestUtil
import org.apache.velocity.app.VelocityEngine
import org.assertj.core.api.Assertions.assertThat
import org.jtwig.environment.EnvironmentConfigurationBuilder
import org.jtwig.functions.FunctionRequest
import org.jtwig.functions.SimpleJtwigFunction
import org.jtwig.util.FunctionValueUtils
import org.junit.Test
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.ResourceCodeResolver
import java.io.File

class TestTemplates {

    private val defaultVelocityEngine = VelocityEngine().apply {
        setProperty("resource.loader", "class")
        setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
    }

    @Test
    fun `velocity templates work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(defaultVelocityEngine)
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test.vm", model("message", "Hello Velocity!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello Velocity!</h1>")
    }

    @Test
    fun `velocity template variables work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(defaultVelocityEngine)
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test-set.vm") }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Set works</h1>")
    }

    @Test
    fun `velocity custom engines work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(defaultVelocityEngine)
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test.vm") }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>\$message</h1>")
        JavalinVelocity.configure(VelocityEngine().apply {
            setProperty("runtime.references.strict", true)
            setProperty("resource.loader", "class")
            setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
        })
        assertThat(http.getBody("/hello")).isEqualTo("Internal server error")
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
        JavalinPebble.configure(PebbleEngine.Builder()
                .loader(ClasspathLoader())
                .strictVariables(true)
                .build())
        assertThat(http.getBody("/hello")).isEqualTo("Internal server error")
    }

    @Test
    fun `jTwig templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/jtwig/test.jtwig", model("message", "Hello jTwig!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello jTwig!</h1>")
    }

    @Test
    fun `jTwig custom engine works`() = TestUtil.test { app, http ->
        JavalinJtwig.configure(EnvironmentConfigurationBuilder.configuration().functions()
                .add(object : SimpleJtwigFunction() {
                    override fun name() = "javalin"
                    override fun execute(request: FunctionRequest): Any {
                        request.maximumNumberOfArguments(1).minimumNumberOfArguments(1)
                        return FunctionValueUtils.getString(request, 0)
                    }
                })
                .and()
                .build()
        )
        app.get("/quiz") { ctx -> ctx.render("/templates/jtwig/custom.jtwig") }
        assertThat(http.getBody("/quiz")).isEqualTo("<h1>Javalin is the best framework you will ever get</h1>")
    }

    @Test
    fun `jte works`() = TestUtil.test { app, http ->
        try {
            JavalinJte.configure(TemplateEngine.create(ResourceCodeResolver("templates/jte"), File("target/jte").toPath(), ContentType.Html))
            app.get("/hello") { ctx -> ctx.render("test.jte", model("page", JteTestPage("hello", "world"))) }
            assertThat(http.getBody("/hello")).isEqualToIgnoringNewLines("<h1>hello world!</h1>")
        } catch (e:UnsupportedClassVersionError) {
            // jte does require JDK 11+ to work
        }
    }

    @Test
    fun `jte multiple params work`() = TestUtil.test { app, http ->
        try {
            JavalinJte.configure(TemplateEngine.create(ResourceCodeResolver("templates/jte"), File("target/jte").toPath(), ContentType.Html))
            app.get("/hello") { ctx -> ctx.render("multiple-params.jte", model("one", "hello", "two", "world")) }
            assertThat(http.getBody("/hello")).isEqualToIgnoringNewLines("<h1>hello world!</h1>")
        } catch (e:UnsupportedClassVersionError) {
            // jte does require JDK 11+ to work
        }
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
        JavalinRenderer.register(FileRenderer { _, _, _ -> "Hah." }, ".lol")
        app.get("/hello") { ctx -> ctx.render("/markdown/test.lol") }
        assertThat(http.getBody("/hello")).isEqualTo("Hah.")
    }

    @Test
    fun `double extension works`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/jtwig/test.html.twig", model("message", "Hello jTwig!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello jTwig!</h1>")
    }

    @Test
    fun `multiple dots in filenames are okay`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/jtwig/multiple.dots.twig", model("message", "Hello jTwig!")) }
        assertThat(http.getBody("/hello")).isEqualTo("<h1>Hello jTwig!</h1>")
    }

    data class JteTestPage(val hello:String, val world:String)
}
