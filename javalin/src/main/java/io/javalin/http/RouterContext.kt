package io.javalin.http

import io.javalin.core.event.EventManager
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.event.WsHandlerMetaInfo
import io.javalin.core.security.AccessManager
import io.javalin.core.security.RouteRole
import io.javalin.core.util.Util.prefixContextPath
import io.javalin.jetty.JavalinJettyServlet
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsHandlerType
import java.util.function.Consumer

class RouterContext(private val javalinServlet: JavalinServlet, private val javalinJettyServlet: JavalinJettyServlet?, private val eventManager: EventManager) {
    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see AccessManager
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole) {
        val roleSet: Set<RouteRole> = setOf(*roles)
        javalinServlet.addHandler(handlerType, path, handler, roleSet)
        eventManager.fireHandlerAddedEvent(HandlerMetaInfo(handlerType, prefixContextPath(javalinServlet.config.contextPath, path), handler, roleSet))
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     *
     * @see AccessManager
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole) {
        val roleSet: Set<RouteRole> = setOf(*roles)
        javalinJettyServlet!!.addHandler(handlerType, path, wsConfig, roleSet)
        eventManager.fireWsHandlerAddedEvent(WsHandlerMetaInfo(handlerType, prefixContextPath(javalinServlet.config.contextPath, path), wsConfig, roleSet))
    }
}
