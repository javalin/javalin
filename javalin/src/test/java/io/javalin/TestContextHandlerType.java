/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin;

import io.javalin.http.HandlerType;
import io.javalin.testing.TestUtil;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.javalin.http.HttpStatus.OK;

public class TestContextHandlerType {

    @Test
    public void testHandlerTypeCanBeAccessedInContext() {
        TestUtil.test(Javalin.create(), (app, http) -> {
            List<HandlerType> handlerTypes = new ArrayList<>();
            app.before(ctx -> handlerTypes.add(ctx.handlerType()));
            app.after(ctx -> handlerTypes.add(ctx.handlerType()));
            app.get("/", ctx -> handlerTypes.add(ctx.handlerType()));

            Assertions.assertThat(http.get("/").getStatus()).isEqualTo(OK.getCode());
            Assertions.assertThat(handlerTypes).containsExactly(HandlerType.BEFORE, HandlerType.GET, HandlerType.AFTER);
        });
    }
}
