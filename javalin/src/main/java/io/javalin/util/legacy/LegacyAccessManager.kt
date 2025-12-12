package io.javalin.util.legacy

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.router.Endpoint
import io.javalin.security.Roles
import io.javalin.security.RouteRole

fun interface LegacyAccessManager {
    fun manage(handler: Handler, ctx: Context, routeRoles: Set<RouteRole>)
}

fun Javalin.legacyAccessManager(legacyAccessManager: LegacyAccessManager): Javalin {
    if (this.jettyServer().started()) throw IllegalStateException("AccessManager must be set before server start")
    this.unsafe.router.handlerWrapper { endpoint: Endpoint ->
        val roles = endpoint.metadata(Roles::class.java)?.roles ?: emptySet()
        when (roles.isEmpty()) {
            true -> endpoint.handler // old access manager didn't run if no roles were present
            false -> Handler { legacyAccessManager.manage(endpoint.handler, it, roles) }
        }
    }
    return this
}
