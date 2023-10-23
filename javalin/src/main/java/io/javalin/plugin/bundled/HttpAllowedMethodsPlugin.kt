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
import io.javalin.router.Endpoint

open class HttpAllowedMethodsPlugin : JavalinPlugin {

    companion object {
        object HttpAllowedMethods : HttpAllowedMethodsPlugin()
    }

    override fun onStart(config: JavalinConfig) {
        config.events.serverStarted {
            config.pvt.internalRouter.allHttpHandlers()
                .asSequence()
                .map { it.endpoint }
                .filter { it.method.isHttpMethod }
                .groupBy({ it.path }, { it.method })
                .mapValues { (_, handlers) -> (handlers + OPTIONS).toSet() }
                .forEach { (path, handlers) ->
                    val allowedMethods = handlers.joinToString(",")

                    config.pvt.internalRouter.addHttpEndpoint(
                        Endpoint(
                            method = OPTIONS,
                            path = path,
                            handler = { it.header(ACCESS_CONTROL_ALLOW_METHODS, allowedMethods) }
                        )
                    )
                }
        }
    }

}
