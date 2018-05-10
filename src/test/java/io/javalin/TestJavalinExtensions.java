/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestJavalinExtensions {

    private static Javalin app;
    private static String origin = null;

    @BeforeClass
    public static void setup() {
        app = Javalin.create()
            .port(0)
            .start();
        origin = "http://localhost:" + app.port();
    }

    @After
    public void clear() {
        app.clearMatcherAndMappers();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @Test
    public void test_helloWorldExtension() throws Exception {
        app.extension(app -> {
            app.before("/protected", ctx -> {
                throw new HaltException(401, "Protected");
            });
            app.get("/", ctx -> ctx.result("Hello world"));
        });

        HttpResponse<String> response = Unirest.get(origin + "/").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("Hello world"));

        HttpResponse<String> response2 = Unirest.get(origin + "/protected").asString();
        assertThat(response2.getStatus(), is(401));
        assertThat(response2.getBody(), is("Protected"));
    }

    @Test
    public void test_javaClassExtension() throws Exception {
        app.extension(new JavaClassExtension("Foobar!"));

        HttpResponse<String> response = Unirest.get(origin + "/").asString();
        assertThat(response.getStatus(), is(400));
        assertThat(response.getBody(), is("Foobar!"));
    }

    static class JavaClassExtension implements Extension {
        private final String magicValue;

        JavaClassExtension(String magicValue) {
            this.magicValue = magicValue;
        }

        String getMagicValue() {
            return magicValue;
        }

        @Override
        public void register(Javalin app) {
            app.before(ctx -> {
                throw new HaltException(400, magicValue);
            });
        }
    }

    @Test
    public void test_javaClassExtensions() {
        app.extension(JavaClassExtension.class, new JavaClassExtension("Foobar!"))
            .extension((app) -> {
                assertThat(app.extension(JavaClassExtension.class).getMagicValue(), is("Foobar!"));
            });
    }

    @Test
    public void test_registerOrderIsFirstComeFirstServe() {
        List<Integer> values = new ArrayList<>();
        app.extension(app -> {
            values.add(1);
        }).extension(app -> {
            values.add(2);
        });

        assertThat(values.get(0), is(1));
        assertThat(values.get(1), is(2));
    }

}
