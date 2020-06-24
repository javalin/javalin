/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.http.Context;
import io.javalin.http.ContextFactory;
import io.javalin.testing.TestUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestContextFactory {

    @Test
    public void contextFactoryWorks() {
        TestUtil.test(
            Javalin.create(config -> config.contextFactory(new MyContextFactory())),
            (app, http) -> {
                app.get("/test", ctx -> {
                    String result = ((MyContext) ctx).foo();
                    ctx.result(result);
                });
                assertThat(http.getBody("/test")).isEqualTo("bar");
            });
    }

    private static class MyContext extends Context {
        public MyContext(@NotNull HttpServletRequest req, @NotNull HttpServletResponse res, @NotNull Map<Class<?>, ?> appAttributes) {
            super(req, res, appAttributes);
        }

        public String foo() {
            return "bar";
        }
    }

    private static class MyContextFactory implements ContextFactory {
        @Override
        public Context createContext(HttpServletRequest request, HttpServletResponse response, Map<Class<?>, Object> appAttributes) {
            return new MyContext(request, response, appAttributes);
        }
    }
}

