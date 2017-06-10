/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security;

import java.util.List;

import io.javalin.Context;
import io.javalin.Handler;

@FunctionalInterface
public interface AccessManager {
    void manage(Handler handler, Context ctx, List<Role> permittedRoles) throws Exception;
}
