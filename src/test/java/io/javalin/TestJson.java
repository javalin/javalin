/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.json.JavalinJackson;
import io.javalin.json.JavalinJson;
import io.javalin.json.JsonToObjectMapper;
import io.javalin.json.ObjectToJsonMapper;
import io.javalin.util.CustomMapper;
import io.javalin.util.TestObject_NonSerializable;
import io.javalin.util.TestObject_Serializable;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestJson extends _UnirestBaseTest {

    @BeforeClass
    public static void setObjectMapper() {
        JavalinJackson.configure(new CustomMapper());
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
        ObjectToJsonMapper oldMapper = JavalinJson.getObjectToJsonMapper(); // reset after test

        JavalinJson.setObjectToJsonMapper(obj -> "Silly mapper");
        app.get("/", ctx -> ctx.json("Test"));
        assertThat(call(HttpMethod.GET, "/").getBody(), is("Silly mapper"));

        JavalinJson.setObjectToJsonMapper(oldMapper);
    }

    @Test
    public void test_customObjectToJsonMapper_normalImplementation_works() throws Exception {
        ObjectToJsonMapper oldMapper = JavalinJson.getObjectToJsonMapper(); // reset after test

        Gson gson = new GsonBuilder().create();
        String expected = gson.toJson(new TestObject_Serializable());
        JavalinJson.setObjectToJsonMapper(gson::toJson);
        app.get("/", ctx -> ctx.json(new TestObject_Serializable()));
        assertThat(call(HttpMethod.GET, "/").getBody(), is(expected));

        JavalinJson.setObjectToJsonMapper(oldMapper);
    }

    @Test
    public void test_customJsonToObjectMapper_sillyImplementation_works() throws Exception {
        JsonToObjectMapper oldMapper = JavalinJson.getJsonToObjectMapper(); // reset after test

        // Map anything to "Silly string"
        String sillyString = "Silly string";
        JavalinJson.setJsonToObjectMapper(new JsonToObjectMapper() {
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

        JavalinJson.setJsonToObjectMapper(oldMapper);
    }

    @Test
    public void test_customJsonToObjectMapper_normalImplementation_works() throws Exception {
        JsonToObjectMapper oldMapper = JavalinJson.getJsonToObjectMapper(); // reset after test

        Gson gson = new GsonBuilder().create();
        JavalinJson.setJsonToObjectMapper(gson::fromJson);
        app.post("/", ctx -> {
            Object o = ctx.bodyAsClass(TestObject_Serializable.class);
            if (o instanceof TestObject_Serializable) {
                ctx.result("success");
            }
        });
        assertThat(Unirest.post(origin + "/").body(gson.toJson(new TestObject_Serializable())).asString().getBody(), is("success"));

        JavalinJson.setJsonToObjectMapper(oldMapper);
    }

}
