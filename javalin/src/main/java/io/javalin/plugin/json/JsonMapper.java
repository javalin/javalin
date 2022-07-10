package io.javalin.plugin.json;

import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

public interface JsonMapper {

    /**
     * Javalin uses this method for {@link io.javalin.http.Context#json(Object)},
     * as well as the CookieStore class, WebSockets messaging, and JavalinVue.
     */
    @NotNull
    default String toJsonString(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJsonString not implemented");
    }

    /**
     * Javalin uses this method for {@link io.javalin.http.Context#json(Object)},
     * if called with useStreamingMapper = true.
     * When implementing this method, use (or look at) PipedStreamUtil to get
     * an InputStream from an OutputStream.
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
     * If implemented, Javalin will use this method instead of {@link #fromJsonString(String, Class)}
     * when mapping request bodies to JSON through {@link io.javalin.http.Context#bodyAsClass(Class)}.
     */
    @NotNull
    default <T> T fromJsonStream(@NotNull InputStream json, @NotNull Class<T> targetClass) {
        throw new NotImplementedError("JsonMapper#fromJsonStream not implemented");
    }

}
