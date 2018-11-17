/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin;

import io.javalin.util.TestUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestExtension {

    @Test
    public void test_lambdaExtension() {
        TestUtil.test((testApp, http) -> {
            testApp.register((app) -> {
                app.before("/protected", ctx -> {
                    throw new UnauthorizedResponse("Protected");
                });
                app.get("/", ctx -> ctx.result("Hello world"));
            });
            assertThat(http.get("/").getStatus(), is(200));
            assertThat(http.get("/").getBody(), is("Hello world"));
            assertThat(http.get("/protected").getStatus(), is(401));
            assertThat(http.get("/protected").getBody(), is("Protected"));
        });
    }

    @Test
    public void test_classExtension() {
        TestUtil.test((app, http) -> {
            app.register(new ClassExtension("Foobar!"));
            assertThat(http.get("/").getStatus(), is(400));
            assertThat(http.get("/").getBody(), is("Foobar!"));
        });
    }

    @Test
    public void register_orderIsFirstComeFirstServe() {
        List<Integer> values = new ArrayList<>();
        Javalin.create()
            .register(app -> values.add(1))
            .register(app -> values.add(2));
        assertThat(values.get(0), is(1));
        assertThat(values.get(1), is(2));
    }

    @Test
    public void test_secondExtensionDependsOnFirst() {
        Javalin.create()
            .register(app -> app.attribute(String.class, "Magic shared value"))
            .register(app -> assertThat(app.attribute(String.class), is("Magic shared value")));
    }

    static class ClassExtension implements Extension {
        private final String magicValue;

        ClassExtension(String magicValue) {
            this.magicValue = magicValue;
        }

        @Override
        public void registerOn(Javalin app) {
            app.before(ctx -> {
                throw new BadRequestResponse(magicValue);
            });
        }
    }

}
