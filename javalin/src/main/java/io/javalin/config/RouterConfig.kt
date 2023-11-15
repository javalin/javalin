package io.javalin.config

import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.router.InternalRouter
import io.javalin.router.JavalinDefaultRouting
import io.javalin.router.JavalinDefaultRouting.Companion.Default
import io.javalin.router.RoutingApiInitializer
import io.javalin.router.exception.JavaLangErrorHandler
import io.javalin.util.JavalinLogger
import java.util.function.Consumer

/**
 * Configuration for the Router.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinConfig.router]
 */
class RouterConfig(internal val cfg: JavalinConfig) {

    // @formatter:off
    /** The context path (ex '/blog' if you are hosting an app on a subpath, like 'mydomain.com/blog') */
    @JvmField var contextPath = "/"
    /** If true, treat '/path' and '/path/' as the same path (default: true). */
    @JvmField var ignoreTrailingSlashes = true
    /** If true, treat '/path//subpath' and '/path/subpath' as the same path (default: false). */
    @JvmField var treatMultipleSlashesAsSingleSlash = false
    /** If true, treat '/PATH' and '/path' as the same path (default: false). */
    @JvmField var caseInsensitiveRoutes = false
    // @formatter:on

    internal var javaLangErrorHandler: JavaLangErrorHandler = JavaLangErrorHandler { res, error ->
        res.status = INTERNAL_SERVER_ERROR.code
        JavalinLogger.error("Fatal error occurred while servicing http-request", error)
    }

    fun <SETUP> mount(initializer: RoutingApiInitializer<SETUP>, setup: Consumer<SETUP> = Consumer {}): RouterConfig = also {
        initializer.initialize(cfg, cfg.pvt.internalRouter, setup)
    }

    fun mount(setup: Consumer<JavalinDefaultRouting>) = mount(Default, setup)

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
