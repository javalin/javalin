package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginPriority
import io.javalin.router.InternalRouter
import io.javalin.security.RouteRole
import java.util.*
import java.util.function.Consumer

/** The route overview plugin provides you with a HTML and/or JSON overview of all the routes registered on your Javalin application. */
class RouteOverviewPlugin(userConfig: Consumer<Config>? = null) : Plugin<RouteOverviewPlugin.Config>(userConfig, Config()) {

    class Config {
        @JvmField var path: String = "/routes"
        @JvmField var roles: Array<out RouteRole> = emptyArray()
    }

    override fun onStart(config: JavalinConfig) {
        config.routes.get(pluginConfig.path, { ctx -> handle(ctx, config.pvt.internalRouter) }, *pluginConfig.roles)
    }

    private fun handle(ctx: Context, internalRouter: InternalRouter) {
        when(ctx.header(Header.ACCEPT)?.lowercase(Locale.ROOT)?.contains(ContentType.JSON)) {
            true -> {
                ctx.header(Header.CONTENT_TYPE, ContentType.JSON)
                ctx.result(RouteOverviewUtil.createJsonOverview(internalRouter.allHttpHandlers(), internalRouter.allWsHandlers()))
            }
            else -> ctx.html(RouteOverviewUtil.createHtmlOverview(internalRouter.allHttpHandlers(), internalRouter.allWsHandlers()))
        }
    }

    override fun priority(): PluginPriority = PluginPriority.LATE

}
