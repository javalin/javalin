package io.javalin.base

import io.javalin.Context
import io.javalin.Handler
import io.javalin.Javalin
import io.javalin.core.HandlerType
import io.javalin.security.AccessManager
import io.javalin.security.Role

internal abstract class JavalinSecuredRoutes : JavalinRoutes() {

    protected var accessManager = AccessManager { _: Handler, _: Context, _: List<Role> ->
        throw IllegalStateException("No access manager configured. Add an access manager using 'accessManager()'")
    }

    protected fun addSecuredHandler(httpMethod: HandlerType, path: String, handler: Handler, permittedRoles: List<Role>) =
            addHandler(httpMethod, path, Handler { ctx -> accessManager.manage(handler, ctx, permittedRoles) })

    override fun accessManager(accessManager: AccessManager): Javalin {
        this.accessManager = accessManager
        return this
    }

    // Secured HTTP verbs
    override fun get(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.GET, path, handler, permittedRoles)

    override fun post(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.POST, path, handler, permittedRoles)

    override fun put(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.PUT, path, handler, permittedRoles)

    override fun patch(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.PATCH, path, handler, permittedRoles)

    override fun delete(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.DELETE, path, handler, permittedRoles)

    override fun head(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.HEAD, path, handler, permittedRoles)

    override fun trace(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.TRACE, path, handler, permittedRoles)

    override fun connect(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.CONNECT, path, handler, permittedRoles)

    override fun options(path: String, handler: Handler, permittedRoles: List<Role>) =
            addSecuredHandler(HandlerType.OPTIONS, path, handler, permittedRoles)
}