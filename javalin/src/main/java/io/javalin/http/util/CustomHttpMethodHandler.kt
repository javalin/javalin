/*
 * Javalin - https://javalin.io
 * Copyright 2024 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.security.RouteRole

/**
 * Utility class for handling custom HTTP methods (e.g., WebDAV methods like PROPFIND, MKCOL).
 * 
 * This provides a cleaner API than manually using before-handlers to check the request method.
 */
object CustomHttpMethodHandler {
    
    /**
     * Handles custom HTTP methods by checking the request method and calling the appropriate handler.
     * If no handler matches, the request continues to other handlers.
     * 
     * @param ctx the request context
     * @param handlers map of method names to handlers
     */
    @JvmStatic
    fun handle(ctx: Context, handlers: Map<String, Handler>) {
        val method = ctx.req().method.uppercase()
        val uppercaseHandlers = handlers.mapKeys { it.key.uppercase() }
        uppercaseHandlers[method]?.let { handler ->
            handler.handle(ctx)
            ctx.skipRemainingHandlers()
        }
    }
}

/**
 * Extension function to handle custom HTTP methods more conveniently.
 */
fun Context.handleCustomMethod(vararg handlers: Pair<String, Handler>) {
    CustomHttpMethodHandler.handle(this, handlers.toMap())
}
