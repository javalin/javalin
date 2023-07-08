package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.PluginPriority
import io.javalin.security.RouteRole
import io.javalin.util.implementingClassName
import io.javalin.util.isClass
import io.javalin.util.isJavaAnonymousLambda
import io.javalin.util.isJavaField
import io.javalin.util.isKotlinAnonymousLambda
import io.javalin.util.isKotlinField
import io.javalin.util.isKotlinMethodReference
import io.javalin.util.javaFieldName
import io.javalin.util.kotlinFieldName
import io.javalin.util.parentClass
import io.javalin.util.runMethod
import java.util.*
import java.util.function.Consumer

object RouteOverviewPluginFactory : PluginFactory<RouteOverviewPlugin, RouteOverviewPluginConfig> {
    override fun create(config: Consumer<RouteOverviewPluginConfig>): RouteOverviewPlugin = RouteOverviewPlugin(config)
}

class RouteOverviewPluginConfig {
    @JvmField var path: String = "/routes"
    @JvmField var roles: Array<out RouteRole> = emptyArray()
}

class RouteOverviewPlugin(config: Consumer<RouteOverviewPluginConfig> = Consumer {}) : JavalinPlugin {

    companion object {
        @JvmStatic val FACTORY = RouteOverviewPluginFactory
    }

    private val config = RouteOverviewPluginConfig().also { config.accept(it) }
    private val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()
    private val wsHandlerMetaInfoList = mutableListOf<WsHandlerMetaInfo>()

    override fun onInitialize(config: JavalinConfig) {
        config.events.handlerAdded { handlerInfo -> handlerMetaInfoList.add(handlerInfo) }
        config.events.wsHandlerAdded { handlerInfo -> wsHandlerMetaInfoList.add(handlerInfo) }
    }

    override fun onStart(app: Javalin) {
        app.get(config.path, this::handle, *config.roles)
    }

    private fun handle(ctx: Context) {
        if (ctx.header(Header.ACCEPT)?.lowercase(Locale.ROOT)?.contains(ContentType.JSON) == true) {
            ctx.header(Header.CONTENT_TYPE, ContentType.JSON)
            ctx.result(RouteOverviewUtil.createJsonOverview(handlerMetaInfoList, wsHandlerMetaInfoList))
        } else {
            ctx.html(RouteOverviewUtil.createHtmlOverview(handlerMetaInfoList, wsHandlerMetaInfoList))
        }
    }

    override fun priority(): PluginPriority = PluginPriority.EARLY

}
