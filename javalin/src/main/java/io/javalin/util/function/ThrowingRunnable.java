package io.javalin.util.function;

/** Throwing version of {@link java.lang.Runnable} **/
@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {

    void run() throws E;

}
