/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.security

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.util.ContextUtil.isLocalhost

object SecurityUtil {

    @JvmStatic
    fun roles(vararg roles: Role) = setOf(*roles)

    @JvmStatic
    fun noopAccessManager(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
        if (permittedRoles.isNotEmpty()) {
            throw IllegalStateException("No access manager configured. Add an access manager using 'Javalin.create(c -> c.accessManager(...))'.")
        }
        handler.handle(ctx)
    }

    @JvmStatic
    fun sslRedirect(ctx: Context) {
        if (ctx.isLocalhost()) return
        val xForwardedProto = ctx.header("x-forwarded-proto")
        if (xForwardedProto == "http" || (xForwardedProto == null && ctx.scheme() == "http")) {
            ctx.redirect(ctx.fullUrl().replace("http", "https"), 301)
        }
    }

}

/**
 * Auth credentials for basic HTTP authorization.
 * Contains the Base64 decoded [username] and [password] from the Authorization header.
 * @see Context.basicAuthCredentials
 */
data class BasicAuthCredentials(val username: String, val password: String)
