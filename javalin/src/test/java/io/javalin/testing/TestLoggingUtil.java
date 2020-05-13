package io.javalin.testing;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestLoggingUtil {
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
