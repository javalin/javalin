// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.embeddedserver;

import javalin.core.ErrorMapper;
import javalin.core.ExceptionMapper;
import javalin.core.PathMatcher;

public interface EmbeddedServerFactory {
    EmbeddedServer create(PathMatcher pathMatcher, ExceptionMapper exceptionMapper, ErrorMapper errorMapper, String staticFileDirectory);
}
