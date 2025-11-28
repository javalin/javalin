package io.javalin.config

import io.javalin.http.RequestLogger
import io.javalin.websocket.WsConfig
import java.util.function.Consumer

/**
 * Configuration for http requests and websocket loggers.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinState.requestLogger]
 */
class RequestLoggerConfig(private val cfg: JavalinState) {

    /** Adds a request logger for HTTP requests. */
    fun http(requestLogger: RequestLogger) {
        cfg.httpRequestLogger = requestLogger
    }

    /** Adds a request logger for websocket requests. */
    fun ws(ws: Consumer<WsConfig>) {
        val logger = WsConfig()
        ws.accept(logger)
        cfg.wsRequestLogger = logger
    }

}
