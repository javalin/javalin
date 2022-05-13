/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit

class HttpAllowedMethodsOnRoutesUtil() : Plugin, PluginLifecycleInit {
    private val endpoints = mutableMapOf<String, MutableSet<HandlerMetaInfo>>()

    override fun init(app: Javalin) {
        app.events {
            it.handlerAdded { handlerInfo ->
                addOptionsToList(handlerInfo)
            }
        }
    }

    override fun apply(app: Javalin) {
        app.events {
            it.serverStarted {
                createOptionsEndPoint(app)
            }
        }
    }

    private fun addOptionsToList(handlerMetaInfo: HandlerMetaInfo) {
        val endpoint = endpoints.getOrPut(handlerMetaInfo.path) { mutableSetOf(handlerMetaInfo) }
        endpoint.add(handlerMetaInfo)
    }

    private fun createOptionsEndPoint(app: Javalin) {
        endpoints.forEach { endpoint ->
            app.options(
                endpoint.key
            ) { context ->
                context.header(
                    Header.ACCESS_CONTROL_ALLOW_METHODS,
                    endpoint.value.joinToString(",") { it.httpMethod.toString() }
                )
            }
        }
    }
}
