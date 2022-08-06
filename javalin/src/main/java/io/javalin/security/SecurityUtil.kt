/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security

import io.javalin.http.Context
import io.javalin.http.Handler

internal object SecurityUtil {

    fun noopAccessManager(handler: Handler, ctx: Context, roles: Set<RouteRole>) {
        if (roles.isNotEmpty()) {
            throw IllegalStateException("No access manager configured. Add an access manager using 'Javalin.create(c -> c.accessManager(...))'.")
        }
        handler.handle(ctx)
    }

}

