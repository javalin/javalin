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
import io.javalin.util.BaseTest
import org.apache.velocity.app.VelocityEngine
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.jtwig.environment.EnvironmentConfigurationBuilder
import org.jtwig.functions.FunctionRequest
import org.jtwig.functions.SimpleJtwigFunction
import org.jtwig.util.FunctionValueUtils
import org.junit.Test

class TestTemplates : BaseTest() {

    @Test
    fun test_renderVelocity_works() {
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test.vm", model("message", "Hello Velocity!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Velocity!</h1>"))
    }

    @Test
    fun test_renderVelocity_works_withSet() {
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test-set.vm") }
        assertThat(http.getBody("/hello"), `is`("<h1>Set works</h1>"))
    }

    @Test
    fun test_customVelocityEngine_works() {
        app.get("/hello") { ctx -> ctx.render("/templates/velocity/test.vm") }
        assertThat(http.getBody("/hello"), `is`("<h1>\$message</h1>"))
        JavalinVelocity.configure(strictVelocityEngine())
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    private fun strictVelocityEngine(): VelocityEngine {
        val strictEngine = VelocityEngine()
        strictEngine.setProperty("runtime.references.strict", true)
        strictEngine.setProperty("resource.loader", "class")
        strictEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
        return strictEngine
    }

    @Test
    fun test_renderFreemarker_works() {
        app.get("/hello") { ctx -> ctx.render("/templates/freemarker/test.ftl", model("message", "Hello Freemarker!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Freemarker!</h1>"))
    }

    @Test
    fun test_renderThymeleaf_works() {
        app.get("/hello") { ctx -> ctx.render("/templates/thymeleaf/test.html", model("message", "Hello Thymeleaf!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Thymeleaf!</h1>"))
    }

    @Test
    fun test_renderMustache_works() {
        app.get("/hello") { ctx -> ctx.render("/templates/mustache/test.mustache", model("message", "Hello Mustache!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Mustache!</h1>"))
    }

    @Test
    fun test_renderPebble_works() {
        app.get("/hello1") { ctx -> ctx.render("templates/pebble/test.peb", model("message", "Hello Pebble!")) }
        assertThat(http.getBody("/hello1"), `is`("<h1>Hello Pebble!</h1>"))
    }

    @Test
    fun test_customPebbleEngine_works() {
        app.get("/hello") { ctx -> ctx.render("templates/pebble/test.peb") }
        assertThat(http.getBody("/hello"), `is`("<h1></h1>"))
        JavalinPebble.configure(strictPebbleEngine())
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    private fun strictPebbleEngine(): PebbleEngine {
        return PebbleEngine.Builder()
                .loader(ClasspathLoader())
                .strictVariables(true)
                .build()
    }

    @Test
    fun test_renderJtwig_works() {
        app.get("/hello") { ctx -> ctx.render("/templates/jtwig/test.jtwig", model("message", "Hello jTwig!")) }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello jTwig!</h1>"))
    }

    @Test
    fun test_customJtwigConfiguration_works() {
        val configuration = EnvironmentConfigurationBuilder
                .configuration().functions()
                .add(object : SimpleJtwigFunction() {
                    override fun name(): String {
                        return "javalin"
                    }

                    override fun execute(request: FunctionRequest): Any {
                        request.maximumNumberOfArguments(1).minimumNumberOfArguments(1)
                        return FunctionValueUtils.getString(request, 0)
                    }
                })
                .and()
                .build()

        JavalinJtwig.configure(configuration)
        app.get("/quiz") { ctx -> ctx.render("/templates/jtwig/custom.jtwig") }
        assertThat(http.getBody("/quiz"), `is`("<h1>Javalin is the best framework you will ever get</h1>"))
    }

    @Test
    fun test_renderMarkdown_works() {
        app.get("/hello") { ctx -> ctx.render("/markdown/test.md") }
        assertThat(http.getBody("/hello"), `is`("<h1>Hello Markdown!</h1>\n"))
    }

    @Test
    fun test_unregisteredExtension_throws() {
        app.get("/hello") { ctx -> ctx.render("/markdown/test.unknown") }
        assertThat(http.getBody("/hello"), `is`("Internal server error"))
    }

    @Test
    fun test_registerCustomRenderer() {
        JavalinRenderer.register(FileRenderer { filePath, model -> "Hah." }, ".lol")
        app.get("/hello") { ctx -> ctx.render("/markdown/test.lol") }
        assertThat(http.getBody("/hello"), `is`("Hah."))
    }

}
