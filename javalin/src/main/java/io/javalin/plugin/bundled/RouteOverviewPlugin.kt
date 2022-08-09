package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.Header
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.security.RouteRole
import java.util.*

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
