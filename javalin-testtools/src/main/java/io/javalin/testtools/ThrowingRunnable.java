package io.javalin.testtools;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}
