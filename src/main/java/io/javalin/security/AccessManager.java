/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security;

import io.javalin.Context;
import io.javalin.Handler;
import java.util.List;

/**
 * Sets authentication and authorization per endpoint.
 *
 * @see Role
 * @see <a href="https://javalin.io/documentation#access-manager">Access manager in docs</a>
 */
@FunctionalInterface
public interface AccessManager {
    void manage(Handler handler, Context ctx, List<Role> permittedRoles) throws Exception;
}
