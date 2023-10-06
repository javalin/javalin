package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.PluginPriority
import io.javalin.router.JavalinDefaultRouting.Companion.Default
import io.javalin.router.InternalRouter
import io.javalin.security.RouteRole
import java.util.*
import java.util.function.Consumer

class RouteOverviewPluginConfig {
    @JvmField var path: String = "/routes"
    @JvmField var roles: Array<out RouteRole> = emptyArray()
}

class RouteOverviewPlugin(config: Consumer<RouteOverviewPluginConfig> = Consumer {}) : JavalinPlugin<RouteOverviewPluginConfig>(
    defaultConfiguration = RouteOverviewPluginConfig(),
    userConfig = config,
    dsl = RouteOverview,
    priority = PluginPriority.LATE,
) {

    companion object {
        @JvmField val RouteOverview = PluginFactory { RouteOverviewPlugin(it) }
    }

    override fun onStart(config: JavalinConfig) {
        config.router.mount(Default) {
            it.get(pluginConfig.path, { ctx -> handle(ctx, config.pvt.internalRouter) }, *pluginConfig.roles)
        }
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

}
