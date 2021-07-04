package io.javalin.plugin.json;

import java.io.InputStream;
import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;

public interface JsonMapper {

    @NotNull
    default String toJsonString(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJsonString not implemented");
    }

    /**
     * If implemented, Javalin will choose this method over toJsonString.
     * Javalin requires an InputStream in order to finish up the response properly.
     * Use (or look at) PipedStreamUtil to get an InputStream from an OutputStream.
     */
    @NotNull
    default InputStream toJsonStream(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJsonStream not implemented");
    }

    @NotNull
    default <T> T fromJsonString(@NotNull String json, @NotNull Class<T> targetClass) {
        throw new NotImplementedError("JsonMapper#fromJsonString not implemented");
    }

    /**
     * If implemented, Javalin will choose this method over fromJsonString.
     */
    @NotNull
    default <T> T fromJsonStream(@NotNull InputStream json, @NotNull Class<T> targetClass) {
        throw new NotImplementedError("JsonMapper#fromJsonStream not implemented");
    }

}
