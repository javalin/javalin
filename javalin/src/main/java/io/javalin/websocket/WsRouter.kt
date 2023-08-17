package io.javalin.websocket

import io.javalin.config.RouterConfig
import io.javalin.security.RouteRole
import java.util.function.Consumer

class WsRouter(private val routerConfig: RouterConfig) {

    val wsExceptionMapper = WsExceptionMapper()
    val wsPathMatcher = WsPathMatcher()

    /** Add a WebSocket handler. */
    fun addHandler(handlerType: WsHandlerType, path: String, ws: Consumer<WsConfig>, roles: Set<RouteRole>) {
        wsPathMatcher.add(
            WsHandlerEntry(
                type = handlerType,
                path = path,
                routerConfig = routerConfig,
                wsConfig = WsConfig().apply { ws.accept(this) },
                roles = roles
            )
        )
    }

}
