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
import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(http.get("/").getStatus()).isEqualTo(200);
            assertThat(http.get("/").getBody()).isEqualTo("Hello world");
            assertThat(http.get("/protected").getStatus()).isEqualTo(401);
            assertThat(http.get("/protected").getBody()).isEqualTo("Protected");
        });
    }

    @Test
    public void test_classExtension() {
        TestUtil.test((app, http) -> {
            app.register(new ClassExtension("Foobar!"));
            assertThat(http.get("/").getStatus()).isEqualTo(400);
            assertThat(http.get("/").getBody()).isEqualTo("Foobar!");
        });
    }

    @Test
    public void register_orderIsFirstComeFirstServe() {
        List<Integer> values = new ArrayList<>();
        Javalin.create()
            .register(app -> values.add(1))
            .register(app -> values.add(2));
        assertThat(values.get(0)).isEqualTo(1);
        assertThat(values.get(1)).isEqualTo(2);
    }

    @Test
    public void test_secondExtensionDependsOnFirst() {
        Javalin.create()
            .register(app -> app.attribute(String.class, "Magic shared value"))
            .register(app -> assertThat(app.attribute(String.class)).isEqualTo("Magic shared value"));
    }

    static class ClassExtension implements Extension {
        private final String magicValue;

        ClassExtension(String magicValue) {
            this.magicValue = magicValue;
        }

        @Override
        public void registerOnJavalin(Javalin app) {
            app.before(ctx -> {
                throw new BadRequestResponse(magicValue);
            });
        }
    }

}
