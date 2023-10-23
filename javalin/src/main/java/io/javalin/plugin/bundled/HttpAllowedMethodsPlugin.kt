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
import io.javalin.plugin.PluginFactory

open class HttpAllowedMethodsPlugin : JavalinPlugin<Unit>(HttpAllowedMethodsPlugin, Unit) {

    companion object {
        @JvmField val HttpAllowedMethodsPlugin = PluginFactory { HttpAllowedMethodsPlugin() }
    }

    override fun onStart(config: JavalinConfig) {
        config.events.serverStarted {
            config.pvt.internalRouter.allHttpHandlers()
                .filter { it.type.isHttpMethod }
                .groupBy({ it.path }, { it.type })
                .mapValues { (_, handlers) -> (handlers + OPTIONS).toSet() }
                .forEach { (path, handlers) ->
                    val allowedMethods = handlers.joinToString(",")

                    config.pvt.internalRouter.addHttpHandler(
                        handlerType = OPTIONS,
                        path = path,
                        handler = { it.header(ACCESS_CONTROL_ALLOW_METHODS, allowedMethods) }
                    )
                }
        }
    }

}
