package io.javalin.websocket

import io.javalin.config.RoutingConfig
import io.javalin.security.RouteRole
import java.util.function.Consumer

class WsRouter(private val routingConfig: RoutingConfig) {

    val wsExceptionMapper = WsExceptionMapper()
    val wsPathMatcher = WsPathMatcher()

    /** Add a WebSocket handler. */
    fun addHandler(handlerType: WsHandlerType, path: String, ws: Consumer<WsConfig>, roles: Set<RouteRole>) {
        wsPathMatcher.add(
            WsEntry(
                type = handlerType,
                path = path,
                routingConfig = routingConfig,
                wsConfig = WsConfig().apply { ws.accept(this) },
                roles = roles
            )
        )
    }

}
