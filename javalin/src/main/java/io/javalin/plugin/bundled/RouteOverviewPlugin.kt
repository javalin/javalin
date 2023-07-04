package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginPriority
import io.javalin.security.RouteRole
import java.util.*

class RouteOverviewPlugin(
    val path: String,
    vararg val roles: RouteRole = arrayOf()
) : JavalinPlugin {

    private val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()
    private val wsHandlerMetaInfoList = mutableListOf<WsHandlerMetaInfo>()

    override fun onInitialize(config: JavalinConfig) {
        config.events.handlerAdded { handlerInfo -> handlerMetaInfoList.add(handlerInfo) }
        config.events.wsHandlerAdded { handlerInfo -> wsHandlerMetaInfoList.add(handlerInfo) }
    }

    override fun onStart(app: Javalin) {
        app.get(path, this::handle, *roles)
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
