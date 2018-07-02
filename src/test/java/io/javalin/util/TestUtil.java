/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.util;

import io.javalin.Javalin;

public class TestUtil {

    public static void test(Javalin javalin, ThrowingBiConsumer<Javalin, HttpUtil> test) {
        javalin.disableStartupBanner().start(0);
        HttpUtil httpUtil = new HttpUtil(javalin);
        test.accept(javalin, httpUtil);
        javalin.stop();
    }

    public static void test(ThrowingBiConsumer<Javalin, HttpUtil> test) {
        test(Javalin.create(), test);
    }

}
