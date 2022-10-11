package io.javalin.util.function;

@FunctionalInterface
public interface ThrowingSupplier<R, E extends Exception> {

    R get() throws E;

}
