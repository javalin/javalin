package io.javalin.config

import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.router.InternalRouter
import io.javalin.router.JavalinDefaultRouting
import io.javalin.router.RoutingApiInitializer
import io.javalin.router.exception.JavaLangErrorHandler
import io.javalin.util.JavalinLogger
import java.util.function.Consumer

class RouterConfig(internal val cfg: JavalinConfig) {

    // @formatter:off
    @JvmField var contextPath = "/"
    @JvmField var ignoreTrailingSlashes = true
    @JvmField var treatMultipleSlashesAsSingleSlash = false
    @JvmField var caseInsensitiveRoutes = false
    // @formatter:on

    internal val initializers = mutableListOf<Runnable>()

    internal var javaLangErrorHandler: JavaLangErrorHandler = JavaLangErrorHandler { res, error ->
        res.status = INTERNAL_SERVER_ERROR.code
        JavalinLogger.error("Fatal error occurred while servicing http-request", error)
    }

    fun <SETUP> mount(initializer: RoutingApiInitializer<SETUP>, setup: Consumer<SETUP> = Consumer {}): RouterConfig = also {
        initializers.add(Runnable { initializer.initialize(cfg, cfg.pvt.internalRouter, setup) })
    }

    fun apiBuilder(endpoints: EndpointGroup): RouterConfig {
        val apiBuilderInitializer = { cfg: JavalinConfig, internalRouter: InternalRouter, setup: Consumer<Void?> ->
            try {
                ApiBuilder.setStaticJavalin(JavalinDefaultRouting(cfg))
                setup.accept(null)
            } finally {
                ApiBuilder.clearStaticJavalin()
            }
        }
        return mount(apiBuilderInitializer) { endpoints.addEndpoints() }
    }


}
