/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.plugin.json.JsonMapper;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

public class HelloWorldGson {

    public static void main(String[] args) {

        Gson gson = new GsonBuilder().create();
        JsonMapper gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj) {
                return gson.toJson(obj);
            }
            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Class<T> targetClass) {
                return gson.fromJson(json, targetClass);
            }
        };

        Javalin app = Javalin.create(config -> config.jsonMapper(gsonMapper)).start(7070);
        app.get("/", ctx -> ctx.json(Arrays.asList("a", "b", "c")));

    }

}
