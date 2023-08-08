package io.javalin.config

import io.javalin.http.router.Router
import io.javalin.http.router.RouterFactory
import java.util.function.Consumer

class RoutingConfig(internal val pvt: PrivateConfig) {

    @JvmField var contextPath = "/"
    @JvmField var ignoreTrailingSlashes = true
    @JvmField var treatMultipleSlashesAsSingleSlash = false
    @JvmField var caseInsensitiveRoutes = false

    fun <ROUTER : Router<ROUTER, SETUP>, SETUP> router(factory: RouterFactory<ROUTER, SETUP>, setup: Consumer<SETUP>): RoutingConfig = also {
        factory.create(pvt.internalRouter, setup)
    }

}
