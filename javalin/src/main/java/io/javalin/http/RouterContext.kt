package io.javalin.http

import io.javalin.core.event.EventManager
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.security.Role
import io.javalin.core.util.Util.isNonSubPathWildcard
import io.javalin.core.util.Util.prefixContextPath

class RouterContext(private val servlet: JavalinServlet, private val eventManager: EventManager) {
    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see AccessManager
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    @JvmOverloads
    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<Role> = setOf()) {
        if (isNonSubPathWildcard(path)) { // TODO: This should probably be made part of the actual path matching
            // split into two handlers: one exact, and one sub-path with wildcard
            val basePath = path.substring(0, path.length - 1)
            addHandler(handlerType, basePath, handler, roles)
            return addHandler(handlerType, "$basePath/*", handler, roles)
        }
        servlet.addHandler(handlerType, path, handler, roles)
        eventManager.fireHandlerAddedEvent(
                HandlerMetaInfo(handlerType, prefixContextPath(servlet.config.contextPath, path), handler, roles)
        )
    }
}
