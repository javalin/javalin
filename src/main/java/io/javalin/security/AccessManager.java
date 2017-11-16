/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security;

import io.javalin.Context;
import io.javalin.Handler;
import java.util.List;

@FunctionalInterface
public interface AccessManager {
    void manage(Handler handler, Context ctx, List<Role> permittedRoles) throws Exception;
}
