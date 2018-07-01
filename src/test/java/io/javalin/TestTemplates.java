/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import io.javalin.util.BaseTest;
import io.javalin.rendering.JavalinRenderer;
import io.javalin.rendering.template.JavalinJtwig;
import io.javalin.rendering.template.JavalinPebble;
import io.javalin.rendering.template.JavalinVelocity;
import org.apache.velocity.app.VelocityEngine;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.util.FunctionValueUtils;
import org.junit.Test;
import static io.javalin.rendering.template.TemplateUtil.model;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestTemplates extends BaseTest {

    @Test
    public void test_renderVelocity_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/velocity/test.vm", model("message", "Hello Velocity!")));
        assertThat(http.getBody("/hello"), is("<h1>Hello Velocity!</h1>"));
    }

    @Test
    public void test_renderVelocity_works_withSet() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/velocity/test-set.vm"));
        assertThat(http.getBody("/hello"), is("<h1>Set works</h1>"));
    }

    @Test
    public void test_customVelocityEngine_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/velocity/test.vm"));
        assertThat(http.getBody("/hello"), is("<h1>$message</h1>"));
        JavalinVelocity.configure(strictVelocityEngine());
        assertThat(http.getBody("/hello"), is("Internal server error"));
    }

    private static VelocityEngine strictVelocityEngine() {
        VelocityEngine strictEngine = new VelocityEngine();
        strictEngine.setProperty("runtime.references.strict", true);
        strictEngine.setProperty("resource.loader", "class");
        strictEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        return strictEngine;
    }

    @Test
    public void test_renderFreemarker_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/freemarker/test.ftl", model("message", "Hello Freemarker!")));
        assertThat(http.getBody("/hello"), is("<h1>Hello Freemarker!</h1>"));
    }

    @Test
    public void test_renderThymeleaf_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/thymeleaf/test.html", model("message", "Hello Thymeleaf!")));
        assertThat(http.getBody("/hello"), is("<h1>Hello Thymeleaf!</h1>"));
    }

    @Test
    public void test_renderMustache_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/mustache/test.mustache", model("message", "Hello Mustache!")));
        assertThat(http.getBody("/hello"), is("<h1>Hello Mustache!</h1>"));
    }

    @Test
    public void test_renderPebble_works() throws Exception {
        app.get("/hello1", ctx -> ctx.render("templates/pebble/test.peb", model("message", "Hello Pebble!")));
        assertThat(http.getBody("/hello1"), is("<h1>Hello Pebble!</h1>"));
    }

    @Test
    public void test_customPebbleEngine_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("templates/pebble/test.peb"));
        assertThat(http.getBody("/hello"), is("<h1></h1>"));
        JavalinPebble.configure(strictPebbleEngine());
        assertThat(http.getBody("/hello"), is("Internal server error"));
    }

    private static PebbleEngine strictPebbleEngine() {
        return new PebbleEngine.Builder()
            .loader(new ClasspathLoader())
            .strictVariables(true)
            .build();
    }

    @Test
    public void test_renderJtwig_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/jtwig/test.jtwig", model("message", "Hello jTwig!")));
        assertThat(http.getBody("/hello"), is("<h1>Hello jTwig!</h1>"));
    }

    @Test
    public void test_customJtwigConfiguration_works() throws Exception {
        EnvironmentConfiguration configuration = EnvironmentConfigurationBuilder
            .configuration().functions()
            .add(new SimpleJtwigFunction() {
                @Override
                public String name() {
                    return "javalin";
                }

                @Override
                public Object execute(FunctionRequest request) {
                    request.maximumNumberOfArguments(1).minimumNumberOfArguments(1);
                    return FunctionValueUtils.getString(request, 0);
                }
            })
            .and()
            .build();

        JavalinJtwig.configure(configuration);
        app.get("/quiz", ctx -> ctx.render("/templates/jtwig/custom.jtwig"));
        assertThat(http.getBody("/quiz"), is("<h1>Javalin is the best framework you will ever get</h1>"));
    }

    @Test
    public void test_renderMarkdown_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/markdown/test.md"));
        assertThat(http.getBody("/hello"), is("<h1>Hello Markdown!</h1>\n"));
    }

    @Test
    public void test_unregisteredExtension_throws() throws Exception {
        app.get("/hello", ctx -> ctx.render("/markdown/test.unknown"));
        assertThat(http.getBody("/hello"), is("Internal server error"));
    }

    @Test
    public void test_registerCustomRenderer() throws Exception {
        JavalinRenderer.register((filePath, model) -> "Hah.", ".lol");
        app.get("/hello", ctx -> ctx.render("/markdown/test.lol"));
        assertThat(http.getBody("/hello"), is("Hah."));
    }

}
