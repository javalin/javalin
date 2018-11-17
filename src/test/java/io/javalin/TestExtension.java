/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin;

import com.mashape.unirest.http.HttpResponse;

import io.javalin.util.TestUtil;
import io.javalin.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;

public class TestExtension {

    @Test
    public void test_lambdaExtension() throws Exception {
        TestUtil.test((testApp, http) -> {
            testApp.register((app) -> {
                app.before("/protected", ctx -> {
                    throw new UnauthorizedResponse("Protected");
                });
                app.get("/", ctx -> ctx.result("Hello world"));
            });

            HttpResponse<String> response = http.get("/");
            assertThat(response.getStatus(), is(200));
            assertThat(response.getBody(), is("Hello world"));

            HttpResponse<String> response2 = http.get("/protected");
            assertThat(response2.getStatus(), is(401));
            assertThat(response2.getBody(), is("Protected"));
        });
    }

    @Test
    public void test_classExtension() throws Exception {
        TestUtil.test((app, http) -> {
            app.register(new ClassExtension("Foobar!"));

            HttpResponse<String> response = http.get("/");
            assertThat(response.getStatus(), is(400));
            assertThat(response.getBody(), is("Foobar!"));
        });
    }

    static class ClassExtension implements Extension {
        private final String magicValue;

        public ClassExtension(String magicValue) {
            this.magicValue = magicValue;
        }

        @Override
        public void addToJavalin(Javalin app) {
            app.before(ctx -> {
                throw new BadRequestResponse(magicValue);
            });
        }
    }

    @Test
    public void register_orderIsFirstComeFirstServe() {
        List<Integer> values = new ArrayList<>();
        Javalin.create()
            .register(app -> { values.add(1); })
            .register(app -> { values.add(2); });

        assertThat(values.get(0), is(1));
        assertThat(values.get(1), is(2));
    }

    @Test
    public void test_secondExtensionDependsOnFirst() {
        Javalin.create()
            .register(app -> {
                app.attribute(String.class, "Magic shared value");
            })
            .register(app -> {
                assertThat(app.attribute(String.class), is("Magic shared value"));
            });
    }

 }
