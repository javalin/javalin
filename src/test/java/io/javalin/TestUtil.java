/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import io.javalin.Handler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.json.JavalinJson;
import io.javalin.util.HttpUtil;
import io.javalin.util.ThrowingBiConsumer;

public class TestUtil {

    public static Handler okHandler = ctx -> ctx.result("OK");

    public static void test(Javalin javalin, ThrowingBiConsumer<Javalin, HttpUtil> test) {
        javalin.config.showJavalinBanner = false;
        javalin.start(0);
        HttpUtil http = new HttpUtil(javalin);
        test.accept(javalin, http);
        javalin.delete("/x-test-cookie-cleaner", ctx -> ctx.cookieMap().keySet().forEach(ctx::removeCookie));
        http.call(HttpMethod.DELETE, "/x-test-cookie-cleaner");
        javalin.stop();
        JavalinJson.setToJsonMapper(JavalinJackson.INSTANCE::toJson);
        JavalinJson.setFromJsonMapper(JavalinJackson.INSTANCE::fromJson);
    }

    public static void test(ThrowingBiConsumer<Javalin, HttpUtil> test) {
        test(Javalin.create(), test);
    }

}
