/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.Context;
import io.javalin.testing.SerializeableObject;
import io.javalin.testing.TestUtil;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

// This feature is for Java users, so the test is written in Java
public class TestContextExtensions {

    @Test
    public void context_extensions_work() {
        TestUtil.test((app, http) -> {
            app.before(ctx -> ctx.register(MyJsonMapper.class, new MyJsonMapper(ctx)));
            app.get("/extended", ctx -> ctx.use(MyJsonMapper.class).toJson(new SerializeableObject()));
            String expected = new GsonBuilder().create().toJson(new SerializeableObject());
            assertThat(http.getBody("/extended")).isEqualTo(expected);
        });
    }

    class MyJsonMapper {
        private Context ctx;
        private Gson gson = new GsonBuilder().create();

        MyJsonMapper(Context ctx) {
            this.ctx = ctx;
        }

        void toJson(Object obj) {
            ctx.result(gson.toJson(obj)).contentType("application/json");
        }
    }

}

