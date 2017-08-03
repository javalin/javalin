/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.junit.Test;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import static io.javalin.ApiBuilder.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class TestApiBuilderTwoServices {

    @Test
    public void testApiBuilder_twoServices() throws Exception {
        Javalin app1 = Javalin.create().setPort(0).start();
        Javalin app2 = Javalin.create().setPort(0).start();
        app1.routes(() -> {
            get("/hello1", ctx -> ctx.result("Hello1"));
        });
        app2.routes(() -> {
            get("/hello1", ctx -> ctx.result("Hello1"));
        });
        app1.routes(() -> {
            get("/hello2", ctx -> ctx.result("Hello2"));
        });
        app2.routes(() -> {
            get("/hello2", ctx -> ctx.result("Hello2"));
        });
        assertThat(call(app1.getPort(), "/hello1"), is("Hello1"));
        assertThat(call(app2.getPort(), "/hello1"), is("Hello1"));
        assertThat(call(app1.getPort(), "/hello2"), is("Hello2"));
        assertThat(call(app2.getPort(), "/hello2"), is("Hello2"));
        app1.stop();
        app2.stop();
    }

    private String call(int port, String path) throws UnirestException {
        return Unirest.get("http://localhost:" + port + path).asString().getBody();
    }

}
