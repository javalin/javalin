/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.config;

import io.javalin.RequestLogger;
import io.javalin.security.AccessManager;
import org.jetbrains.annotations.NotNull;

public interface SamConversionsServlet {
    void accessManager(@NotNull AccessManager accessManager);

    void requestLogger(@NotNull RequestLogger requestLogger);
}
