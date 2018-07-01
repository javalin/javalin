/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMultipleInstances {

    @Test
    public void test_getMultiple() throws UnirestException {
        Javalin app1 = Javalin.create().start(0).get("/hello-1", ctx -> ctx.result("Hello first World"));
        Javalin app2 = Javalin.create().start(0).get("/hello-2", ctx -> ctx.result("Hello second World"));
        Javalin app3 = Javalin.create().start(0).get("/hello-3", ctx -> ctx.result("Hello third World"));
        assertThat(Unirest.get("http://localhost:" + app1.port() + "/hello-1").asString().getBody(), is("Hello first World"));
        assertThat(Unirest.get("http://localhost:" + app2.port() + "/hello-2").asString().getBody(), is("Hello second World"));
        assertThat(Unirest.get("http://localhost:" + app3.port() + "/hello-3").asString().getBody(), is("Hello third World"));
        app1.stop();
        app2.stop();
        app3.stop();
    }

}
