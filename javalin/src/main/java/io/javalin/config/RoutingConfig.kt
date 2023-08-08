package io.javalin.config

import io.javalin.router.Router
import io.javalin.router.RouterFactory
import java.util.function.Consumer

class RoutingConfig(internal val cfg: JavalinConfig) {

    @JvmField var contextPath = "/"
    @JvmField var ignoreTrailingSlashes = true
    @JvmField var treatMultipleSlashesAsSingleSlash = false
    @JvmField var caseInsensitiveRoutes = false

    fun <ROUTER : Router<ROUTER, SETUP>, SETUP> router(factory: RouterFactory<ROUTER, SETUP>, setup: Consumer<SETUP>): RoutingConfig = also {
        factory.create(cfg.pvt.internalRouter, setup)
    }

}
