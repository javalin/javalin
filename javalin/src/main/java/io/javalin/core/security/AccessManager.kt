/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.core.security

import io.javalin.http.Context
import io.javalin.http.Handler

/**
 * The access manager is a way of implementing per-endpoint security management.
 * It's only enabled for endpoints if a list of roles is provided.
 * Ex: get("/secured", SecuredController::get, roles(LOGGED_IN));
 *
 * @see Role
 *
 * @see io.javalin.Javalin.addHandler
 * @see [Access manager in docs](https://javalin.io/documentation.access-manager)
 */
fun interface AccessManager {
    @Throws(Exception::class)
    fun manage(handler: Handler, ctx: Context, permittedRoles: Set<Role>)
}
