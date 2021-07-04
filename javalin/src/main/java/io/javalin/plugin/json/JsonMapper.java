package io.javalin.plugin.json;

import java.io.InputStream;
import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;

public interface JsonMapper {

    /**
     * If {@link #toJsonStream(Object)} is not implemented, Javalin will use this method
     * when sending JSON data to a client through {@link io.javalin.http.Context#json(Object)}.
     * Regardless of if {@link #toJsonStream(Object)} is implemented, Javalin will use this
     * method in the CookieStore class, for WebSockets messaging, and in JavalinVue.
     */
    @NotNull
    default String toJsonString(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJsonString not implemented");
    }

    /**
     * You can implement this method to reduce memory usage.
     *
     * If implemented, Javalin will use this method instead of {@link #toJsonString(Object)}
     * when sending JSON data to a client through {@link io.javalin.http.Context#json(Object)}.
     * Javalin requires an InputStream in order to finish up the response properly.
     * Use (or look at) PipedStreamUtil to get an InputStream from an OutputStream.
     */
    @NotNull
    default InputStream toJsonStream(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJsonStream not implemented");
    }

    /**
     * If {@link #fromJsonStream(InputStream, Class)} is not implemented, Javalin will use this method
     * when mapping request bodies to JSON through {@link io.javalin.http.Context#bodyAsClass(Class)}.
     * Regardless of if {@link #fromJsonStream(InputStream, Class)} is implemented, Javalin will
     * use this method for Validation and for WebSocket messaging.
     */
    @NotNull
    default <T> T fromJsonString(@NotNull String json, @NotNull Class<T> targetClass) {
        throw new NotImplementedError("JsonMapper#fromJsonString not implemented");
    }

    /**
     * You can implement this method to reduce memory usage.
     *
     * If implemented, Javalin will use this method instead of {@link #fromJsonString(String, Class)}
     * when mapping request bodies to JSON through {@link io.javalin.http.Context#bodyAsClass(Class)}.
     */
    @NotNull
    default <T> T fromJsonStream(@NotNull InputStream json, @NotNull Class<T> targetClass) {
        throw new NotImplementedError("JsonMapper#fromJsonStream not implemented");
    }

}
