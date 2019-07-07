/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering;

import io.javalin.http.Context;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for creating renderers to be used with {@link Context#render}.
 */
@FunctionalInterface
public interface FileRenderer {
    String render(@NotNull String filePath, @NotNull Map<String, Object> model, @NotNull Context context) throws Exception;
}
