/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import io.javalin.translator.json.JavalinJacksonPlugin;
import io.javalin.translator.json.JavalinJsonPlugin;
import io.javalin.translator.json.JsonToObjectMapper;
import io.javalin.translator.json.ObjectToJsonMapper;
import io.javalin.translator.template.JavalinJtwigPlugin;
import io.javalin.translator.template.JavalinPebblePlugin;
import io.javalin.translator.template.JavalinVelocityPlugin;
import io.javalin.util.CustomMapper;
import io.javalin.util.TestObject_NonSerializable;
import io.javalin.util.TestObject_Serializable;
import org.apache.velocity.app.VelocityEngine;
import org.jetbrains.annotations.NotNull;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.util.FunctionValueUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import static io.javalin.translator.template.TemplateUtil.model;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestTranslators extends _UnirestBaseTest {

    @BeforeClass
    public static void setObjectMapper() {
        JavalinJacksonPlugin.configure(new CustomMapper());
    }

    @Test
    public void test_json_jacksonMapsObjectToJson() throws Exception {
        app.get("/hello", ctx -> ctx.json(new TestObject_Serializable()));
        String expected = new CustomMapper().writeValueAsString(new TestObject_Serializable());
        assertThat(GET_body("/hello"), is(expected));
    }

    @Test
    public void test_json_jacksonMapsStringsToJson() throws Exception {
        app.get("/hello", ctx -> ctx.json("\"ok\""));
        assertThat(GET_body("/hello"), is("\"\\\"ok\\\"\""));
    }

    @Test
    public void test_json_customMapper_works() throws Exception {
        app.get("/hello", ctx -> ctx.json(new TestObject_Serializable()));
        assertThat(GET_body("/hello").split("\r\n|\r|\n").length, is(4));
    }

    @Test
    public void test_json_jackson_throwsForBadObject() throws Exception {
        app.get("/hello", ctx -> ctx.json(new TestObject_NonSerializable()));
        HttpResponse<String> response = call(HttpMethod.GET, "/hello");
        assertThat(response.getStatus(), is(500));
        assertThat(response.getBody(), is("Internal server error"));
    }

    @Test
    public void test_json_jacksonMapsJsonToObject() throws Exception {
        app.post("/hello", ctx -> {
            Object o = ctx.bodyAsClass(TestObject_Serializable.class);
            if (o instanceof TestObject_Serializable) {
                ctx.result("success");
            }
        });
        HttpResponse<String> response = Unirest.post(origin + "/hello").body(new CustomMapper().writeValueAsString(new TestObject_Serializable())).asString();
        assertThat(response.getBody(), is("success"));
    }

    @Test
    public void test_json_jacksonMapsJsonToObject_throwsForBadObject() throws Exception {
        app.get("/hello", ctx -> ctx.json(ctx.bodyAsClass(TestObject_NonSerializable.class).getClass().getSimpleName()));
        HttpResponse<String> response = call(HttpMethod.GET, "/hello");
        assertThat(response.getStatus(), is(500));
        assertThat(response.getBody(), is("Internal server error"));
    }

    @Test
    public void test_customObjectToJsonMapper_sillyImplementation_works() throws Exception {
        ObjectToJsonMapper oldMapper = JavalinJsonPlugin.getObjectToJsonMapper(); // reset after test

        JavalinJsonPlugin.setObjectToJsonMapper(obj -> "Silly mapper");
        app.get("/", ctx -> ctx.json("Test"));
        assertThat(call(HttpMethod.GET, "/").getBody(), is("Silly mapper"));

        JavalinJsonPlugin.setObjectToJsonMapper(oldMapper);
    }

    @Test
    public void test_customObjectToJsonMapper_normalImplementation_works() throws Exception {
        ObjectToJsonMapper oldMapper = JavalinJsonPlugin.getObjectToJsonMapper(); // reset after test

        Gson gson = new GsonBuilder().create();
        String expected = gson.toJson(new TestObject_Serializable());
        JavalinJsonPlugin.setObjectToJsonMapper(gson::toJson);
        app.get("/", ctx -> ctx.json(new TestObject_Serializable()));
        assertThat(call(HttpMethod.GET, "/").getBody(), is(expected));

        JavalinJsonPlugin.setObjectToJsonMapper(oldMapper);
    }

    @Test
    public void test_customJsonToObjectMapper_sillyImplementation_works() throws Exception {
        JsonToObjectMapper oldMapper = JavalinJsonPlugin.getJsonToObjectMapper(); // reset after test

        // Map anything to "Silly string"
        String sillyString = "Silly string";
        JavalinJsonPlugin.setJsonToObjectMapper(new JsonToObjectMapper() {
            public <T> T map(@NotNull String json, @NotNull Class<T> targetClass) {
                return (T) sillyString;
            }
        });
        app.post("/", ctx -> {
            if (sillyString.equals(ctx.bodyAsClass(String.class))) {
                ctx.result(sillyString);
            }
        });
        assertThat(Unirest.post(origin + "/").body("{}").asString().getBody(), is(sillyString));

        JavalinJsonPlugin.setJsonToObjectMapper(oldMapper);
    }

    @Test
    public void test_customJsonToObjectMapper_normalImplementation_works() throws Exception {
        JsonToObjectMapper oldMapper = JavalinJsonPlugin.getJsonToObjectMapper(); // reset after test

        Gson gson = new GsonBuilder().create();
        JavalinJsonPlugin.setJsonToObjectMapper(gson::fromJson);
        app.post("/", ctx -> {
            Object o = ctx.bodyAsClass(TestObject_Serializable.class);
            if (o instanceof TestObject_Serializable) {
                ctx.result("success");
            }
        });
        assertThat(Unirest.post(origin + "/").body(gson.toJson(new TestObject_Serializable())).asString().getBody(), is("success"));

        JavalinJsonPlugin.setJsonToObjectMapper(oldMapper);
    }

    @Test
    public void test_renderVelocity_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/velocity/test.vm", model("message", "Hello Velocity!")));
        assertThat(GET_body("/hello"), is("<h1>Hello Velocity!</h1>"));
    }

    @Test
    public void test_renderVelocity_works_withSet() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/velocity/test-set.vm"));
        assertThat(GET_body("/hello"), is("<h1>Set works</h1>"));
    }

    @Test
    public void test_customVelocityEngine_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/velocity/test.vm"));
        assertThat(GET_body("/hello"), is("<h1>$message</h1>"));
        JavalinVelocityPlugin.configure(strictVelocityEngine());
        assertThat(GET_body("/hello"), is("Internal server error"));
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
        assertThat(GET_body("/hello"), is("<h1>Hello Freemarker!</h1>"));
    }

    @Test
    public void test_renderThymeleaf_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/thymeleaf/test.html", model("message", "Hello Thymeleaf!")));
        assertThat(GET_body("/hello"), is("<h1>Hello Thymeleaf!</h1>"));
    }

    @Test
    public void test_renderMustache_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/templates/mustache/test.mustache", model("message", "Hello Mustache!")));
        assertThat(GET_body("/hello"), is("<h1>Hello Mustache!</h1>"));
    }

    @Test
    public void test_renderPebble_works() throws Exception {
        app.get("/hello1", ctx -> ctx.render("templates/pebble/test.peb", model("message", "Hello Pebble!")));
        assertThat(GET_body("/hello1"), is("<h1>Hello Pebble!</h1>"));
    }

    @Test
    public void test_customPebbleEngine_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("templates/pebble/test.peb"));
        assertThat(GET_body("/hello"), is("<h1></h1>"));
        JavalinPebblePlugin.configure(strictPebbleEngine());
        assertThat(GET_body("/hello"), is("Internal server error"));
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
        assertThat(GET_body("/hello"), is("<h1>Hello jTwig!</h1>"));
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

        JavalinJtwigPlugin.configure(configuration);
        app.get("/quiz", ctx -> ctx.render("/templates/jtwig/custom.jtwig"));
        assertThat(GET_body("/quiz"), is("<h1>Javalin is the best framework you will ever get</h1>"));
    }

    @Test
    public void test_renderMarkdown_works() throws Exception {
        app.get("/hello", ctx -> ctx.render("/markdown/test.md"));
        assertThat(GET_body("/hello"), is("<h1>Hello Markdown!</h1>\n"));
    }

}
