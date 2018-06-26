/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering;

import io.javalin.Context;
import java.util.Map;

/**
 * Interface for creating renderers to be used with {@link Context#render}.
 */
@FunctionalInterface
public interface FileRenderer {
    String render(String filePath, Map<String, Object> model);
}
