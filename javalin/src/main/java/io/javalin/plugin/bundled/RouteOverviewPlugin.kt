package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginConfiguration
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.PluginPriority
import io.javalin.plugin.createUserConfig
import io.javalin.router.InternalRouter
import io.javalin.security.RouteRole
import java.util.*
import java.util.function.Consumer

class RouteOverviewPluginConfig : PluginConfiguration {
    @JvmField var path: String = "/routes"
    @JvmField var roles: Array<out RouteRole> = emptyArray()
}

class RouteOverviewPlugin(config: Consumer<RouteOverviewPluginConfig> = Consumer {}) : JavalinPlugin {

    open class RouteOverview : PluginFactory<RouteOverviewPlugin, RouteOverviewPluginConfig> {
        override fun create(config: Consumer<RouteOverviewPluginConfig>): RouteOverviewPlugin = RouteOverviewPlugin(config)
    }

    companion object {
        object RouteOverview : RouteOverviewPlugin.RouteOverview()
    }

    private val config = config.createUserConfig(RouteOverviewPluginConfig())

    override fun onStart(app: Javalin) {
        app.get(config.path, { handle(it, app.unsafeConfig().pvt.internalRouter) }, *config.roles)
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
