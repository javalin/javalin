package io.javalin.util.function;

/** Throwing version of {@link java.util.function.Consumer} */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;
}

