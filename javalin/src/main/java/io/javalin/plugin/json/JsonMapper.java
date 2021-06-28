package io.javalin.plugin.json;

import kotlin.NotImplementedError;
import org.jetbrains.annotations.NotNull;

public interface JsonMapper {
    default @NotNull <T> T fromJson(@NotNull String json, @NotNull Class<T> targetClass) {
        throw new NotImplementedError("JsonMapper#fromJson not implemented");
    }
    default @NotNull String toJson(@NotNull Object obj) {
        throw new NotImplementedError("JsonMapper#toJson not implemented");
    }
}
