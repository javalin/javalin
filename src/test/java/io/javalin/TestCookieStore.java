/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.util.BaseTest;
import io.javalin.rendering.template.TemplateUtil;
import io.javalin.misc.TestObject_Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestCookieStore extends BaseTest {

    @Test
    public void test_cookieStore_betweenHandlers() throws Exception {
        app.get("/cookie-store", ctx -> ctx.cookieStore("test-object", new TestObject_Serializable()));
        app.after("/cookie-store", ctx -> {
            if (ctx.cookieStore("test-object") instanceof TestObject_Serializable) {
                ctx.result("Got stored value from different handler");
            }
        });
        assertThat(http.getBody_withCookies("/cookie-store"), is("Got stored value from different handler"));
    }

    @Test
    public void test_cookieStore_clear() throws Exception {
        app.get("/cookie-storer", ctx -> ctx.cookieStore("test-object", new TestObject_Serializable()));
        app.get("/cookie-clearer", Context::clearCookieStore);
        app.get("/cookie-checker", ctx -> ctx.result("stored: " + ctx.cookie("javalin-cookie-store")));
        http.getBody_withCookies("/cookie-storer");
        http.getBody_withCookies("/cookie-clearer");
        assertThat(http.getBody_withCookies("/cookie-checker"), is("stored: null"));
    }

    @Test
    public void test_cookieStore_betweenRequests() throws Exception {
        app.get("/cookie-storer", ctx -> ctx.cookieStore("test-object", new TestObject_Serializable()));
        app.get("/cookie-reader", ctx -> {
            if (ctx.cookieStore("test-object") instanceof TestObject_Serializable) {
                ctx.result("Got stored value from different request");
            }
        });
        http.getBody_withCookies("/cookie-storer");
        assertThat(http.getBody_withCookies("/cookie-reader"), is("Got stored value from different request"));
    }

    @Test
    public void test_cookieStore_betweenRequests_withStateOverwrite() throws Exception {
        app.get("/cookie-storer", ctx -> ctx.cookieStore("test-object", new TestObject_Serializable()));
        app.after("/cookie-storer", ctx -> ctx.cookieStore("test-object-2", new TestObject_Serializable()));
        app.get("/cookie-reader", ctx -> {
            if (ctx.cookieStore("test-object") instanceof TestObject_Serializable && ctx.cookieStore("test-object-2") instanceof TestObject_Serializable) {
                ctx.result("Got stored value from two different handlers on different request");
            }
        });
        http.getBody_withCookies("/cookie-storer");
        assertThat(http.getBody_withCookies("/cookie-reader"), is("Got stored value from two different handlers on different request"));
    }

    @Test
    public void test_cookieStore_betweenRequests_withObjectOverwrite() throws Exception {
        app.get("/cookie-storer", ctx -> ctx.cookieStore("test-object", new TestObject_Serializable()));
        app.get("/cookie-overwriter", ctx -> ctx.cookieStore("test-object", "Hello world!"));
        app.get("/cookie-reader", ctx -> {
            if ("Hello world!".equals(ctx.cookieStore("test-object"))) {
                ctx.result("Overwrote cookie from previous request");
            }
        });
        http.getBody_withCookies("/cookie-storer");
        http.getBody_withCookies("/cookie-overwriter");
        assertThat(http.getBody_withCookies("/cookie-reader"), is("Overwrote cookie from previous request"));
    }

    @Test
    public void test_cookieStore_betweenRequests_multipleObjects() throws Exception {
        app.get("/cookie-storer", ctx -> {
            ctx.cookieStore("s", "Hello world!");
            ctx.cookieStore("i", 42);
            ctx.cookieStore("d", 42d);
            ctx.cookieStore("l", Arrays.asList("One", "Two", "Three"));
            ctx.cookieStore("m", TemplateUtil.model("K1", "V", "K2", 1000d, "K3", Arrays.asList("One", "Two", "Three")));
        });
        app.get("/cookie-reader", ctx -> {
            String s = ctx.cookieStore("s");
            int i = ctx.cookieStore("i");
            double d = ctx.cookieStore("d");
            List l = ctx.cookieStore("l");
            Map m = ctx.cookieStore("m");
            ctx.result(s + " " + i + " " + d + " " + l + " " + m);
        });
        http.getBody_withCookies("/cookie-storer");
        assertThat(http.getBody_withCookies("/cookie-reader"), is("Hello world! 42 42.0 [One, Two, Three] {K1=V, K2=1000.0, K3=[One, Two, Three]}"));
    }

}
