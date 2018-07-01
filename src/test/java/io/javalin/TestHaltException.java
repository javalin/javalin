/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.util.BaseTest;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TestHaltException extends BaseTest {

    @Test
    public void test_haltBeforeWildcard_works() throws Exception {
        app.before("/admin/*", ctx -> {
            throw new HaltException(401);
        });
        app.get("/admin/protected", ctx -> ctx.result("Protected resource"));
        assertThat(http.get("/admin/protected").code(), is(401));
        assertThat(http.getBody("/admin/protected"), not("Protected resource"));
    }

    @Test
    public void test_haltInRoute_works() throws Exception {
        app.get("/some-route", ctx -> {
            throw new HaltException(401, "Stop!");
        });
        assertThat(http.get("/some-route").code(), is(401));
        assertThat(http.getBody("/some-route"), is("Stop!"));
    }

    @Test
    public void test_afterRuns_afterHalt() throws Exception {
        app.get("/some-route", ctx -> {
            throw new HaltException(401, "Stop!");
        }).after(ctx -> {
            ctx.status(418);
        });
        assertThat(http.get("/some-route").code(), is(418));
        assertThat(http.getBody("/some-route"), is("Stop!"));
    }

    @Test
    public void test_constructorsWork() {
        HaltException haltException1 = new HaltException();
        HaltException haltException2 = new HaltException(401);
        HaltException haltException3 = new HaltException("Body");
        HaltException haltException4 = new HaltException(401, "Body");
    }

}
