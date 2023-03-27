/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.security

import io.javalin.http.Context
import io.javalin.http.Handler
import kotlin.Throws
import java.lang.Exception
import java.lang.IllegalStateException

/**
 * The access manager is a way of implementing per-endpoint security management.
 * It's only enabled for endpoints if a list of roles is provided.
 * Ex: get("/secured", SecuredController::get, roles(LOGGED_IN));
 *
 * @see RouteRole
 * @see io.javalin.Javalin.addHandler
 * @see [Access manager in docs](https://javalin.io/documentation.access-manager)
 */
fun interface AccessManager {

    /**
     * @param handler http handler to call as follows `handler.handle(ctx)` if request is authenticated
     * @param ctx current context
     * @param routeRoles configured roles for this route
     */
    @Throws(Exception::class)
    fun manage(handler: Handler, ctx: Context, routeRoles: Set<RouteRole>)

}

fun accessManagerNotConfiguredException() =
    IllegalStateException("No access-manager configured. Add an access-manager using 'Javalin.create(c -> c.accessManager(...))'.")
