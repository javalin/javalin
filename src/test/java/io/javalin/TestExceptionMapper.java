/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.util.BaseTest;
import io.javalin.misc.TypedException;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestExceptionMapper extends BaseTest {

    @Test
    public void test_unmappedException_caughtByGeneralHandler() throws Exception {
        app.get("/unmapped-exception", ctx -> {
            throw new Exception();
        });
        assertThat(http.get("/unmapped-exception").code(), is(500));
        assertThat(http.getBody("/unmapped-exception"), is("Internal server error"));
    }

    @Test
    public void test_mappedException_isHandled() throws Exception {
        app.get("/mapped-exception", ctx -> {
            throw new Exception();
        }).exception(Exception.class, (e, ctx) -> ctx.result("It's been handled."));
        assertThat(http.get("/mapped-exception").code(), is(200));
        assertThat(http.getBody("/mapped-exception"), is("It's been handled."));
    }

    @Test
    public void test_typedMappedException_isHandled() throws Exception {
        app.get("/typed-exception", ctx -> {
            throw new TypedException();
        }).exception(TypedException.class, (e, ctx) -> {
            ctx.result(e.proofOfType());
        });
        assertThat(http.get("/typed-exception").code(), is(200));
        assertThat(http.getBody("/typed-exception"), is("I'm so typed"));
    }

    @Test
    public void test_moreSpecificException_isHandledFirst() throws Exception {
        app.get("/exception-priority", ctx -> {
            throw new TypedException();
        }).exception(Exception.class, (e, ctx) -> {
            ctx.result("This shouldn't run");
        }).exception(TypedException.class, (e, ctx) -> {
            ctx.result("Typed!");
        });
        assertThat(http.get("/exception-priority").code(), is(200));
        assertThat(http.getBody("/exception-priority"), is("Typed!"));
    }

}
