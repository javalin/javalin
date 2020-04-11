/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.security;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The access manager is a way of implementing per-endpoint security management.
 * It's only enabled for endpoints if a list of roles is provided.
 * Ex: get("/secured", SecuredController::get, roles(LOGGED_IN));
 *
 * @see Role
 * @see io.javalin.Javalin#addHandler(HandlerType, String, Handler, Set)
 * @see <a href="https://javalin.io/documentation#access-manager">Access manager in docs</a>
 */
@FunctionalInterface
public interface AccessManager {
    void manage(@NotNull Handler handler, @NotNull Context ctx, @NotNull Set<Role> permittedRoles) throws Exception;
}
