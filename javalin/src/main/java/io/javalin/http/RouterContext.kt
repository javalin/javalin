package io.javalin.http

import io.javalin.core.event.EventManager
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.event.WsHandlerMetaInfo
import io.javalin.core.security.Role
import io.javalin.core.util.Util.isNonSubPathWildcard
import io.javalin.core.util.Util.prefixContextPath
import io.javalin.websocket.JavalinWsServlet
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsHandlerType
import java.util.function.Consumer

class RouterContext(private val servlet: JavalinServlet, private val wsServlet: JavalinWsServlet?, private val eventManager: EventManager) {
    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see AccessManager
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

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     *
     * @see AccessManager
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    @JvmOverloads
    fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, roles: Set<Role> = setOf()) {
        wsServlet!!.addHandler(handlerType, path, wsConfig, roles)
        eventManager.fireWsHandlerAddedEvent(WsHandlerMetaInfo(handlerType, prefixContextPath(servlet.config.contextPath, path), wsConfig, roles))
    }
}
