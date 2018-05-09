/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.Unirest;
import io.javalin.util.TestObject_Serializable;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestContextExtensions {

    @Test
    public void test_jsonMapper_extension() throws Exception {
        Javalin app = Javalin.start(0);
        app.before(ctx -> {
            ctx.extension(MyJsonMapper.class, new MyJsonMapper(ctx));
        });
        app.get("/extended", ctx -> {
            ctx.extension(MyJsonMapper.class).toJson(new TestObject_Serializable());
        });
        String response = Unirest.get("http://localhost:" + app.port() + "/extended").asString().getBody();
        String expected = new GsonBuilder().create().toJson(new TestObject_Serializable());
        assertThat(response, is(expected));
        app.stop();
    }

    class MyJsonMapper {
        private Context ctx;
        private Gson gson = new GsonBuilder().create();

        public MyJsonMapper(Context ctx) {
            this.ctx = ctx;
        }

        public void toJson(Object obj) {
            this.ctx.result(gson.toJson(obj));
        }
    }

}

