package io.javalin.router

import java.util.function.Consumer

interface RoutingApi<ROUTER : RoutingApi<ROUTER, SETUP>, SETUP>

interface RouterFactory<ROUTER : RoutingApi<ROUTER, SETUP>, SETUP > {
    fun create(internalRouter: InternalRouter<*>, setup: Consumer<SETUP>): ROUTER
}
