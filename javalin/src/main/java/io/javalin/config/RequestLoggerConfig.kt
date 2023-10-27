package io.javalin.config

import io.javalin.http.RequestLogger
import io.javalin.websocket.WsConfig
import java.util.function.Consumer

/**
 * Configuration for http requests and websocket loggers.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinConfig.requestLogger]
 */
class RequestLoggerConfig(private val cfg: JavalinConfig) {

    /**
     * Adds a request logger for HTTP requests.
     * @param requestLogger the request logger
     */
    fun http(requestLogger: RequestLogger) {
        cfg.pvt.requestLogger = requestLogger
    }

    /**
     * Adds a request logger for websocket requests.
     * @param ws the request logger
     */
    fun ws(ws: Consumer<WsConfig>) {
        val logger = WsConfig()
        ws.accept(logger)
        cfg.pvt.wsLogger = logger
    }

}
