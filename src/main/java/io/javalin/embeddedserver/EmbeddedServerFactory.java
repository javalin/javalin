/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.embeddedserver;

import io.javalin.core.ErrorMapper;
import io.javalin.core.ExceptionMapper;
import io.javalin.core.PathMatcher;

public interface EmbeddedServerFactory {
    EmbeddedServer create(PathMatcher pathMatcher, ExceptionMapper exceptionMapper, ErrorMapper errorMapper, String staticFileDirectory);
}
