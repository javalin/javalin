/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.testing;

import io.javalin.Javalin;
import io.javalin.config.Key;
import io.javalin.http.Handler;
import io.javalin.util.JavalinLogger;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestUtil {

    public static Boolean captureLogs = true;

    public static Handler okHandler = ctx -> ctx.result("OK");

    public static Key<String> TestLogsKey = new Key<>("testlogs");

    public static void test(ThrowingBiConsumer<Javalin, HttpUtil> test) {
        test(Javalin.create(), test);
    }

    public static void test(Javalin app, ThrowingBiConsumer<Javalin, HttpUtil> userCode) {
        testWithResult(app, userCode);
    }

    public static RunResult testWithResult(ThrowingBiConsumer<Javalin, HttpUtil> test) {
        return testWithResult(Javalin.create(), test);
    }

    public static RunResult testWithResult(Javalin app, ThrowingBiConsumer<Javalin, HttpUtil> userCode) {
        RunResult result = runAndCaptureLogs(() -> {
            app.start(0);
            HttpUtil http = new HttpUtil(app.port());
            userCode.accept(app, http);
            app.delete("/x-test-cookie-cleaner", ctx -> ctx.cookieMap().keySet().forEach(ctx::removeCookie));
            http.call("DELETE", "/x-test-cookie-cleaner");
            app.stop();
        });
        app.unsafeConfig().appData(TestLogsKey, result.logs);
        if (result.exception != null) {
            JavalinLogger.error("TestUtil#test failed - full log output below:\n" + result.logs);
            throw new RuntimeException(result.exception);
        }
        return result;
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
        } catch (Throwable t) {
            if (t instanceof Exception) exception = (Exception) t;
            else if (t instanceof AssertionError) exception = new Exception("Assertion error: " + t.getMessage());
            else exception = new Exception("Unexpected Throwable in test. Message: '" + t.getMessage() + "'", t);
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
            JavalinLogger.error("TestUtil#runLogLess failed - full log output below:\n" + result.logs);
            throw new RuntimeException(result.exception);
        }
    }

    public static String captureStdOut(Runnable run) {
        return runAndCaptureLogs(run).logs;
    }

}
