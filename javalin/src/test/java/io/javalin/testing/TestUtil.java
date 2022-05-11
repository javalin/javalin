/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.testing;

import io.javalin.Javalin;
import io.javalin.core.util.JavalinLogger;
import io.javalin.http.Handler;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import kong.unirest.HttpMethod;

public class TestUtil {

    public static Boolean captureLogs = true;

    public static Handler okHandler = ctx -> ctx.result("OK");

    public static void test(ThrowingBiConsumer<Javalin, HttpUtil> test) {
        test(Javalin.create(), test);
    }

    public static void test(Javalin app, ThrowingBiConsumer<Javalin, HttpUtil> userCode) {
        RunResult result = runAndCaptureLogs(() -> {
            app.start(0);
            HttpUtil http = new HttpUtil(app.port());
            userCode.accept(app, http);
            app.delete("/x-test-cookie-cleaner", ctx -> ctx.cookieMap().keySet().forEach(ctx::removeCookie));
            http.call(HttpMethod.DELETE, "/x-test-cookie-cleaner");
            app.stop();
        });
        app.attribute("testlogs", result.logs);
        if (result.exception != null) {
            JavalinLogger.error("There were non-assertion errors in test code.\n" + result.logs);
            throw new RuntimeException(result.exception);
        }
    }

    public static RunResult runAndCaptureLogs(Runnable testCode) {
        Exception exception = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        if (captureLogs) {
            System.setOut(printStream);
            System.setErr(printStream);
        }
        try {
            testCode.run();
        } catch (Exception e) {
            exception = e;
        } finally {
            System.out.flush();
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        return new RunResult(out.toString(), exception);
    }

    public static void runLogLess(Runnable run) {
        RunResult result = runAndCaptureLogs(run);
        if (result.exception != null) {
            JavalinLogger.error("There were non-assertion errors in test code.\n" + result.logs);
            throw new RuntimeException(result.exception);
        }
    }

    public static String captureStdOut(Runnable run) {
        return runAndCaptureLogs(run).logs;
    }

}
