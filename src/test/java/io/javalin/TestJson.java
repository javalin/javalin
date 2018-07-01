/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.Unirest;
import io.javalin.json.FromJsonMapper;
import io.javalin.json.JavalinJackson;
import io.javalin.json.JavalinJson;
import io.javalin.newutil.BaseTest;
import io.javalin.util.CustomMapper;
import io.javalin.util.TestObject_NonSerializable;
import io.javalin.util.TestObject_Serializable;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestJson extends BaseTest {

    @Before
    public void setObjectMapper() {
        JavalinJackson.configure(new CustomMapper()); // reset for every test
    }

    @Test
    public void test_json_jacksonMapsObjectToJson() throws Exception {
        app.get("/hello", ctx -> ctx.json(new TestObject_Serializable()));
        String expected = new CustomMapper().writeValueAsString(new TestObject_Serializable());
        assertThat(http.getBody("/hello"), is(expected));
    }

    @Test
    public void test_json_jacksonMapsStringsToJson() throws Exception {
        app.get("/hello", ctx -> ctx.json("\"ok\""));
        assertThat(http.getBody("/hello"), is("\"\\\"ok\\\"\""));
    }

    @Test
    public void test_json_customMapper_works() throws Exception {
        app.get("/hello", ctx -> ctx.json(new TestObject_Serializable()));
        assertThat(http.getBody("/hello").split("\r\n|\r|\n").length, is(4));
    }

    @Test
    public void test_json_jackson_throwsForBadObject() throws Exception {
        app.get("/hello", ctx -> ctx.json(new TestObject_NonSerializable()));
        assertThat(http.get("/hello").code(), is(500));
        assertThat(http.getBody("/hello"), is("Internal server error"));
    }

    @Test
    public void test_json_jacksonMapsJsonToObject() throws Exception {
        app.post("/hello", ctx -> {
            Object o = ctx.bodyAsClass(TestObject_Serializable.class);
            if (o instanceof TestObject_Serializable) {
                ctx.result("success");
            }
        });
        String jsonString = new CustomMapper().writeValueAsString(new TestObject_Serializable());
        assertThat(http.post("/hello").body(jsonString).asString().getBody(), is("success"));
    }

    @Test
    public void test_json_jacksonMapsJsonToObject_throwsForBadObject() throws Exception {
        app.get("/hello", ctx -> ctx.json(ctx.bodyAsClass(TestObject_NonSerializable.class).getClass().getSimpleName()));
        assertThat(http.get("/hello").code(), is(500));
        assertThat(http.getBody("/hello"), is("Internal server error"));
    }

    @Test
    public void test_customObjectToJsonMapper_sillyImplementation_works() throws Exception {
        JavalinJson.setToJsonMapper(obj -> "Silly mapper");
        app.get("/", ctx -> ctx.json("Test"));
        assertThat(http.getBody("/"), is("Silly mapper"));
    }

    @Test
    public void test_customObjectToJsonMapper_normalImplementation_works() throws Exception {
        Gson gson = new GsonBuilder().create();
        String expected = gson.toJson(new TestObject_Serializable());
        JavalinJson.setToJsonMapper(gson::toJson);
        app.get("/", ctx -> ctx.json(new TestObject_Serializable()));
        assertThat(http.getBody_withCookies("/"), is(expected));
    }

    @Test
    public void test_customJsonToObjectMapper_sillyImplementation_works() throws Exception {
        String sillyString = "Silly string";
        JavalinJson.setFromJsonMapper(new FromJsonMapper() {
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
    }

    @Test
    public void test_customJsonToObjectMapper_normalImplementation_works() throws Exception {
        Gson gson = new GsonBuilder().create();
        JavalinJson.setFromJsonMapper(gson::fromJson);
        app.post("/", ctx -> {
            Object o = ctx.bodyAsClass(TestObject_Serializable.class);
            if (o instanceof TestObject_Serializable) {
                ctx.result("success");
            }
        });
        assertThat(http.post("/").body(gson.toJson(new TestObject_Serializable())).asString().getBody(), is("success"));
    }

}
