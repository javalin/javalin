package io.javalin.plugin.routeoverview

import io.javalin.Javalin
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.event.WsHandlerMetaInfo
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.security.RouteRole
import io.javalin.http.Header
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Handler
import java.util.Locale

class RouteOverviewPlugin(
    val path: String,
    vararg val roles: RouteRole = arrayOf()
) : Plugin, PluginLifecycleInit {

    private lateinit var renderer: RouteOverviewRenderer

    override fun init(app: Javalin) {
        this.renderer = RouteOverviewRenderer(app)
    }

    override fun apply(app: Javalin) {
        app.get(path, renderer, *roles)
    }

}

class RouteOverviewRenderer(val app: Javalin) : Handler {

    private val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()
    private val wsHandlerMetaInfoList = mutableListOf<WsHandlerMetaInfo>()

    init {
        app.events { it.handlerAdded { handlerInfo -> handlerMetaInfoList.add(handlerInfo) } }
        app.events { it.wsHandlerAdded { handlerInfo -> wsHandlerMetaInfoList.add(handlerInfo) } }
    }

    override fun handle(ctx: Context) {
        if (ctx.header(Header.ACCEPT)?.lowercase(Locale.ROOT)?.contains(ContentType.JSON) == true) {
            ctx.header(Header.CONTENT_TYPE, ContentType.JSON)
            ctx.result(RouteOverviewUtil.createJsonOverview(handlerMetaInfoList, wsHandlerMetaInfoList))
        } else {
            ctx.html(RouteOverviewUtil.createHtmlOverview(handlerMetaInfoList, wsHandlerMetaInfoList))
        }
    }

}
