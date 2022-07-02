package io.javalin.core.config

import io.javalin.http.RequestLogger
import io.javalin.websocket.WsConfig
import java.util.function.Consumer

class LoggingConfig(private val inner: InnerConfig) {

    fun http(requestLogger: RequestLogger) {
        inner.requestLogger = requestLogger
    }

    fun webSocket(ws: Consumer<WsConfig>) {
        val logger = WsConfig()
        ws.accept(logger)
        inner.wsLogger = logger
    }

}
