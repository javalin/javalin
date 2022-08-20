package io.javalin.config

import io.javalin.http.RequestLogger
import io.javalin.websocket.WsConfig
import java.util.function.Consumer

class RequestLoggerConfig(private val pvt: PrivateConfig) {

    fun http(requestLogger: RequestLogger) {
        pvt.requestLogger = requestLogger
    }

    fun ws(ws: Consumer<WsConfig>) {
        val logger = WsConfig()
        ws.accept(logger)
        pvt.wsLogger = logger
    }

}
