/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Arrays;

public class HelloWorldCustomJsonMapper {

    public static void main(String[] args) {
        JsonMapper rawJsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                // serialize obj your favourite api
                return "{ \"" + type.getTypeName() + "\": \"" + obj + "\" }";
            }

            @NotNull
            @Override
            @SuppressWarnings("unchecked")
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                // deserialize json your favourite api
                if (targetType.equals(String.class)) {
                    return (T) json;
                } else {
                    throw new UnsupportedOperationException("RawJsonMapper can deserialize only strings");
                }
            }
        };

        Javalin.create(config -> {
            config.jsonMapper(rawJsonMapper);
            config.routes.get("/", ctx -> ctx.json(Arrays.asList("a", "b", "c")));
        }).start(7070);
    }

}
