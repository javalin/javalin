package io.javalin.router

import io.javalin.config.JavalinConfig
import java.util.function.Consumer

interface RoutingApi<ROUTER : RoutingApi<ROUTER, SETUP>, SETUP>

interface RouterFactory<ROUTER : RoutingApi<ROUTER, SETUP>, SETUP > {
    fun create(cfg: JavalinConfig, internalRouter: InternalRouter<*>, setup: Consumer<SETUP>): ROUTER
}
