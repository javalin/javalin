/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security

import io.javalin.Context
import io.javalin.Handler

object SecurityUtil {

    @JvmStatic
    fun roles(vararg roles: Role) = setOf(*roles)

    @JvmStatic
    fun noopAccessManager(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
        if (permittedRoles.isNotEmpty()) {
            throw IllegalStateException("No access manager configured. Add an access manager using 'app.configure(c -> c.accessManager(...))'.")
        }
        handler.handle(ctx)
    }

    @JvmStatic
    fun sslRedirect(ctx: Context) {
        if (ctx.scheme() == "http") {
            ctx.redirect(ctx.fullUrl().replace("http", "https"), 301)
        }
    }

}

internal enum class CoreRoles : Role { NO_WRAP } // used to avoid wrapping CORS options
