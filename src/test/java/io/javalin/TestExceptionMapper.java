/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import io.javalin.util.TypedException;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestExceptionMapper extends _UnirestBaseTest {

    @Test
    public void test_unmappedException_caughtByGeneralHandler() throws Exception {
        app.get("/unmapped-exception", ctx -> {
            throw new Exception();
        });
        HttpResponse<String> response = GET_asString("/unmapped-exception");
        assertThat(response.getBody(), is("Internal server error"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void test_mappedException_isHandled() throws Exception {
        app.get("/mapped-exception", ctx -> {
            throw new Exception();
        }).exception(Exception.class, (e, ctx) -> ctx.result("It's been handled."));
        HttpResponse<String> response = GET_asString("/mapped-exception");
        assertThat(response.getBody(), is("It's been handled."));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void test_typedMappedException_isHandled() throws Exception {
        app.get("/typed-exception", ctx -> {
            throw new TypedException();
        }).exception(TypedException.class, (e, ctx) -> {
            ctx.result(e.proofOfType());
        });
        HttpResponse<String> response = GET_asString("/typed-exception");
        assertThat(response.getBody(), is("I'm so typed"));
        assertThat(response.getStatus(), is(200));
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
        HttpResponse<String> response = GET_asString("/exception-priority");
        assertThat(response.getBody(), is("Typed!"));
        assertThat(response.getStatus(), is(200));
    }

}
