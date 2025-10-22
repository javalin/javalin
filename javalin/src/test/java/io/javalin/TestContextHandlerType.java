/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.router.Endpoint;
import io.javalin.testing.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.javalin.http.HttpStatus.OK;
import static io.javalin.testing.JavalinTestUtil.after;
import static io.javalin.testing.JavalinTestUtil.before;
import static io.javalin.testing.JavalinTestUtil.get;
import static org.assertj.core.api.Assertions.assertThat;

public class TestContextHandlerType {

    @Test
    public void testHandlerTypeCanBeAccessedInContext() {
        TestUtil.test(Javalin.create(), (app, http) -> {
            List<HandlerType> handlerTypes = new ArrayList<>();
            before(app, ctx -> handlerTypes.add(ctx.endpoint().method));
            get(app, "/", ctx -> handlerTypes.add(ctx.endpoint().method));
            after(app, ctx -> handlerTypes.add(ctx.endpoint().method));
            assertThat(http.get("/").getStatus()).isEqualTo(OK.getCode());
            assertThat(handlerTypes).containsExactly(HandlerType.BEFORE, HandlerType.GET, HandlerType.AFTER);
        });
    }

}
