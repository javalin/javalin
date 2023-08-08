package io.javalin.config

import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.router.exception.JavaLangErrorHandler
import io.javalin.router.RoutingApi
import io.javalin.router.RouterFactory
import io.javalin.util.JavalinLogger
import java.util.function.Consumer

class RouterConfig(internal val cfg: JavalinConfig) {

    @JvmField var contextPath = "/"
    @JvmField var ignoreTrailingSlashes = true
    @JvmField var treatMultipleSlashesAsSingleSlash = false
    @JvmField var caseInsensitiveRoutes = false

    internal var javaLangErrorHandler: JavaLangErrorHandler = JavaLangErrorHandler { res, error ->
        res.status = INTERNAL_SERVER_ERROR.code
        JavalinLogger.error("Fatal error occurred while servicing http-request", error)
    }

    fun <ROUTER : RoutingApi<ROUTER, SETUP>, SETUP> routing(factory: RouterFactory<ROUTER, SETUP>, setup: Consumer<SETUP>): RouterConfig = also {
        factory.create(cfg, cfg.pvt.internalRouter, setup)
    }

}
