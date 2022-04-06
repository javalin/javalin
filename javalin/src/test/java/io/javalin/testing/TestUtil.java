/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.testing;

import io.javalin.Javalin;
import io.javalin.core.util.JavalinLogger;
import io.javalin.http.Handler;
import kong.unirest.HttpMethod;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestUtil {

    public static Handler okHandler = ctx -> ctx.result("OK");

    public static void test(Javalin javalin, ThrowingBiConsumer<Javalin, HttpUtil> test) {
        JavalinLogger.enabled = false;
        try (final Javalin closingJavalin = javalin.start(0)) {
            HttpUtil http = new HttpUtil(closingJavalin.port());
            test.accept(closingJavalin, http);
            closingJavalin.delete("/x-test-cookie-cleaner", ctx -> ctx.cookieMap().keySet().forEach(ctx::removeCookie));
            http.call(HttpMethod.DELETE, "/x-test-cookie-cleaner");
        }
        JavalinLogger.enabled = true;
    }

    public static void test(ThrowingBiConsumer<Javalin, HttpUtil> test) {
        test(Javalin.create(), test);
    }

    public static String captureStdOut(Runnable run) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        System.setOut(printStream);
        System.setErr(printStream);
        try {
            run.run();
        } finally {
            System.out.flush();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        return out.toString();
    }

}
