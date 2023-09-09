/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_METHODS
import io.javalin.plugin.JavalinPlugin

open class HttpAllowedMethodsPlugin : JavalinPlugin {

    companion object {
        object HttpAllowedMethods : HttpAllowedMethodsPlugin()
    }

    override fun onStart(config: JavalinConfig) {
        config.events.serverStarted {
            config.pvt.internalRouter.allHttpHandlers()
                .filter { it.type.isHttpMethod }
                .groupBy { it.path }
                .forEach { (path, handlers) ->
                    val allowedMethods = handlers.joinToString(",") { it.type.toString() }

                    config.pvt.internalRouter.addHttpHandler(
                        handlerType = OPTIONS,
                        path = path,
                        handler = { ctx -> ctx.header(ACCESS_CONTROL_ALLOW_METHODS, allowedMethods) }
                    )
                }
        }
    }

}
