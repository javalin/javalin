/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.util;

import io.javalin.Javalin;

public class TestUtil {

    private final HttpUtil httpUtil;
    private final Javalin javalin;

    public TestUtil() { // could add BiConsumer directly to constructor...
        this.javalin = Javalin.create().disableStartupBanner().start(0);
        this.httpUtil = new HttpUtil(javalin);
    }

    public TestUtil(Javalin javalin) {
        this.javalin = javalin.disableStartupBanner().start(0);
        this.httpUtil = new HttpUtil(javalin);
    }

    public void test(ThrowingBiConsumer<Javalin, HttpUtil> app) {
        app.accept(javalin, httpUtil);
        javalin.stop();
    }

}
