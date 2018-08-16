/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.JavalinRenderer
import io.javalin.rendering.template.JavalinJtwig
import io.javalin.rendering.template.JavalinPebble
import io.javalin.rendering.template.JavalinVelocity
import io.javalin.rendering.template.TemplateUtil.model
import io.javalin.util.TestUtil
import org.apache.velocity.app.VelocityEngine
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.jtwig.environment.EnvironmentConfigurationBuilder
import org.jtwig.functions.FunctionRequest
import org.jtwig.functions.SimpleJtwigFunction
import org.jtwig.util.FunctionValueUtils
import org.junit.Test

class TestTemplates {

    private val defaultVelocityEngine = VelocityEngine().apply {
        setProperty("resource.loader", "class")
        setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
    }

    @Test
    fun `velocity templates work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(defaultVelocityEngine)
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test.vm", model("message", "Hello Velocity!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Velocity!</h1>"))
    }

    @Test
    fun `velocity template variables work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(defaultVelocityEngine)
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test-set.vm") }
        assertThat(http.getBody("/hello"), `is`("<h1>Set works</h1>"))
    }

    @Test
    fun `velocity custom engines work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(defaultVelocityEngine)
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test.vm") }
        assertThat(http.getBody("/hello"), `is`("<h1>\$message</h1>"))
        JavalinVelocity.configure(VelocityEngine().apply {
            setProperty("runtime.references.strict", true)
            setProperty("resource.loader", "class")
            setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
        })
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun `velocity external templates work`() = TestUtil.test { app, http ->
        JavalinVelocity.configure(VelocityEngine().apply {
            setProperty("file.resource.loader.path", "src/test/resources/templates/velocity");
        })
        app.get("/hello") { ctx -> ctx.render("test.vm") }
        assertThat(http.getBody("/hello"), `is`("<h1>\$message</h1>"))
    }

    @Test
    fun `freemarker templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/freemarker/test.ftl", model("message", "Hello Freemarker!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Freemarker!</h1>"))
    }

    @Test
    fun `thymeleaf templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/thymeleaf/test.html", model("message", "Hello Thymeleaf!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Thymeleaf!</h1>"))
    }

    @Test
    fun `mustache templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/mustache/test.mustache", model("message", "Hello Mustache!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Mustache!</h1>"))
    }

    @Test
    fun `pebble templates work`() = TestUtil.test { app, http ->
        app.get("/hello1") { ctx -> ctx.render("templates/pebble/test.peb", model("message", "Hello Pebble!")) }
        assertThat(http.getBody("/hello1"), `is`("<h1>Hello Pebble!</h1>"))
    }

    @Test
    fun `pebble custom engines work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("templates/pebble/test.peb") }
        assertThat(http.getBody("/hello"), `is`("<h1></h1>"))
        JavalinPebble.configure(PebbleEngine.Builder()
                .loader(ClasspathLoader())
                .strictVariables(true)
                .build())
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun `jTwig templates work`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/templates/jtwig/test.jtwig", model("message", "Hello jTwig!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello jTwig!</h1>"))
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
        assertThat(http.getBody("/quiz"), `is`("<h1>Javalin is the best framework you will ever get</h1>"))
    }

    @Test
    fun `markdown works`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/markdown/test.md") }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Markdown!</h1>\n"))
    }

    @Test
    fun `unregistered extension throws exception`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.render("/markdown/test.unknown") }
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun `registering custom renderer works`() = TestUtil.test { app, http ->
        JavalinRenderer.register(FileRenderer { filePath, model -> "Hah." }, ".lol")
        app.get("/hello") { ctx -> ctx.render("/markdown/test.lol") }
        assertThat(http.getBody("/hello"), `is`("Hah."))
    }

}
