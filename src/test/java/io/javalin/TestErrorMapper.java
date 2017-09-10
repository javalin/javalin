/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestErrorMapper extends _UnirestBaseTest {

    @Test
    public void test_404mapper_works() throws Exception {
        app.error(404, ctx -> {
            ctx.result("Custom 404 page");
        });
        assertThat(GET_body("/unmapped"), is("Custom 404 page"));
    }

    @Test
    public void test_500mapper_works() throws Exception {
        app.get("/exception", ctx -> {
            throw new RuntimeException();
        }).error(500, ctx -> {
            ctx.result("Custom 500 page");
        });
        assertThat(GET_body("/exception"), is("Custom 500 page"));
    }

    @Test
    public void testError_higherPriority_thanException() throws Exception {
        app.get("/exception", ctx -> {
            throw new RuntimeException();
        }).exception(Exception.class, (e, ctx) -> {
            ctx.status(500).result("Exception handled!");
        }).error(500, ctx -> {
            ctx.result("Custom 500 page");
        });
        assertThat(GET_body("/exception"), is("Custom 500 page"));
    }

    @Test
    public void testError_throwingException_isCaughtByExceptionMapper() throws Exception {
        app.get("/exception", ctx -> {
            throw new RuntimeException();
        }).exception(Exception.class, (e, ctx) -> {
            ctx.status(500).result("Exception handled!");
        }).error(500, ctx -> {
            ctx.result("Custom 500 page");
            throw new RuntimeException();
        });
        assertThat(GET_body("/exception"), is("Exception handled!"));
    }

}
