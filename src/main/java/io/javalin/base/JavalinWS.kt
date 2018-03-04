package io.javalin.base

import io.javalin.Javalin
import io.javalin.embeddedserver.jetty.websocket.WebSocketConfig
import io.javalin.embeddedserver.jetty.websocket.WebSocketHandler
import java.util.HashMap

internal abstract class JavalinWS : JavalinSecuredRoutes() {

    // WebSockets
    // Only available via Jetty, as there is no WebSocket interface in Java to build on top of

    protected val pathWsHandlers = HashMap<String, Any>()

    private fun addWebSocketHandler(path: String, webSocketObject: Any): Javalin {
        ensureActionIsPerformedBeforeServerStart("Configuring WebSockets")
        pathWsHandlers[path] = webSocketObject
        return this
    }

    override fun ws(path: String, ws: WebSocketConfig): Javalin {
        val configuredHandler = WebSocketHandler()
        ws.configure(configuredHandler)
        return addWebSocketHandler(path, configuredHandler)
    }

    override fun ws(path: String, webSocketClass: Class<*>): Javalin {
        return addWebSocketHandler(path, webSocketClass)
    }

    override fun ws(path: String, webSocketObject: Any): Javalin {
        return addWebSocketHandler(path, webSocketObject)
    }
}