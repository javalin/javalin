/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.apache.velocity.app.VelocityEngine;
import org.junit.BeforeClass;
import org.junit.Test;

import io.javalin.translator.json.JavalinJacksonPlugin;
import io.javalin.translator.template.JavalinVelocityPlugin;
import io.javalin.translator.template.TemplateUtil;
import io.javalin.util.CustomMapper;
import io.javalin.util.TestObject_NonSerializable;
import io.javalin.util.TestObject_Serializable;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestTranslators extends _UnirestBaseTest {

    @BeforeClass
    public static void setObjectMapper() {
        JavalinJacksonPlugin.configure(new CustomMapper());
    }

    @Test
    public void test_json_jacksonMapsObjectToJson() throws Exception {
        app.get("/hello", ctx -> ctx.status(200).json(new TestObject_Serializable()));
        String expected = new CustomMapper().writeValueAsString(new TestObject_Serializable());
        assertThat(GET_body("/hello"), is(expected));
    }

    @Test
    public void test_json_jacksonMapsStringsToJson() throws Exception {
        app.get("/hello", ctx -> ctx.status(200).json("\"ok\""));
        assertThat(GET_body("/hello"), is("\"\\\"ok\\\"\""));
    }

    @Test
    public void test_json_customMapper_works() throws Exception {
        app.get("/hello", ctx -> ctx.status(200).json(new TestObject_Serializable()));
        assertThat(GET_body("/hello").split("\r\n|\r|\n").length, is(4));
    }

    @Test
    public void test_json_jackson_throwsForBadObject() throws Exception {
        app.get("/hello", ctx -> ctx.status(200).json(new TestObject_NonSerializable()));
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
    public void test_renderVelocity_works() throws Exception {
        app.get("/hello", ctx -> ctx.renderVelocity("/templates/velocity/test.vm", TemplateUtil.model("message", "Hello Velocity!")));
        assertThat(GET_body("/hello"), is("<h1>Hello Velocity!</h1>"));
    }

    @Test
    public void test_customVelocityEngine_works() throws Exception {
        app.get("/hello", ctx -> ctx.renderVelocity("/templates/velocity/test.vm"));
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
        app.get("/hello", ctx -> ctx.renderFreemarker("/templates/freemarker/test.ftl", TemplateUtil.model("message", "Hello Freemarker!")));
        assertThat(GET_body("/hello"), is("<h1>Hello Freemarker!</h1>"));
    }

    @Test
    public void test_renderThymeleaf_works() throws Exception {
        app.get("/hello", ctx -> ctx.renderThymeleaf("/templates/thymeleaf/test.html", TemplateUtil.model("message", "Hello Thymeleaf!")));
        assertThat(GET_body("/hello"), is("<h1>Hello Thymeleaf!</h1>"));
    }

    @Test
    public void test_renderMustache_works() throws Exception {
        app.get("/hello", ctx -> ctx.renderMustache("/templates/mustache/test.mustache", TemplateUtil.model("message", "Hello Mustache!")));
        assertThat(GET_body("/hello"), is("<h1>Hello Mustache!</h1>"));
    }

}
