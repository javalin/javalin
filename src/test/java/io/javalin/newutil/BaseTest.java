/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.newutil;

import io.javalin.Handler;
import io.javalin.Javalin;
import org.junit.After;
import org.junit.Before;

public class BaseTest {

    public Handler okHandler = ctx -> ctx.result("OK");

    public Javalin app;
    public HttpUtil http;
    public String origin;

    @Before
    public void setup() {
        app = Javalin.create().disableStartupBanner().start(0);
        http = new HttpUtil(app);
        origin = "http://localhost:" + app.port();
    }

    @After
    public void clear() {
        app.stop();
    }

}
