/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpMethod;
import io.javalin.Javalin;
import io.javalin.core.util.Util;
import io.javalin.http.Handler;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JavalinJson;
import org.slf4j.LoggerFactory;

public class TestUtil {

    public static Handler okHandler = ctx -> ctx.result("OK");

    public static void test(Javalin javalin, ThrowingBiConsumer<Javalin, HttpUtil> test) {
        Util.INSTANCE.setLogIfNotStarted(false);
        javalin.config.showJavalinBanner = false;
        Javalin.log = LoggerFactory.getLogger(Javalin.class);
        javalin.start(0);
        Javalin.log = null;
        HttpUtil http = new HttpUtil(javalin.port());
        test.accept(javalin, http);
        javalin.delete("/x-test-cookie-cleaner", ctx -> ctx.cookieMap().keySet().forEach(ctx::removeCookie));
        http.call(HttpMethod.DELETE, "/x-test-cookie-cleaner");
        Javalin.log = LoggerFactory.getLogger(Javalin.class);
        javalin.stop();
        JavalinJackson.configure(new ObjectMapper());
        JavalinJson.setToJsonMapper(JavalinJackson.INSTANCE::toJson);
        JavalinJson.setFromJsonMapper(JavalinJackson.INSTANCE::fromJson);
    }

    public static void test(ThrowingBiConsumer<Javalin, HttpUtil> test) {
        test(Javalin.create(), test);
    }

}
