package io.javalin.testtools;

import io.javalin.Javalin;
import io.javalin.core.util.JavalinLogger;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class TestUtil {

    static boolean disableJavalinLogging = true;

    public static void test(Javalin javalin, ThrowingBiConsumer<Javalin, HttpClient> test) {
        if (disableJavalinLogging) {
            JavalinLogger.enabled = false;
        }
        javalin.start(0);
        HttpClient http = new HttpClient(javalin.port());
        test.accept(javalin, http);
        javalin.stop();
        if (disableJavalinLogging) {
            JavalinLogger.enabled = true;
        }
    }

    public static void test(ThrowingBiConsumer<Javalin, HttpClient> test) {
        test(Javalin.create(), test);
    }

    public static String captureStdOut(ThrowingRunnable run) {
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
