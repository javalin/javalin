/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.misc.SerializeableObject;
import io.javalin.util.TestUtil;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestContextExtensions {

    @Test
    public void context_extensions_work() {
        TestUtil.test((app, http) -> {
            app.before(ctx -> ctx.register(MyJsonMapper.class, new MyJsonMapper(ctx)));
            app.get("/extended", ctx -> ctx.use(MyJsonMapper.class).toJson(new SerializeableObject()));
            String expected = new GsonBuilder().create().toJson(new SerializeableObject());
            assertThat(http.getBody("/extended"), is(expected));
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

