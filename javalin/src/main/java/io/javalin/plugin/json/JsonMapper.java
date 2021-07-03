package io.javalin.plugin.json;

import java.io.InputStream;
import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;

public interface JsonMapper {

    @NotNull
    default <T> T fromJson(@NotNull String json, @NotNull Class<T> targetClass) {
        throw new NotImplementedError("JsonMapper#fromJson not implemented");
    }

    @NotNull
    default String toJson(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJson not implemented");
    }

    /**
     * Javalin needs an InputStream in order to finish up the response
     * properly, which can be a little challenging if your JSON library
     * wants to write to an OutputStream. You can look at JavalinJackson,
     * which uses a PipedStreamUtil to get an InputStream from an OutputStream.
     */
    @NotNull
    default InputStream toJsonStream(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJsonStream not implemented");
    }

}
