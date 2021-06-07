package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.security.RouteRole

class RouteOverviewPlugin(val config: RouteOverviewConfig) : Plugin, PluginLifecycleInit {
    @JvmOverloads
    constructor(path: String, vararg roles: RouteRole = arrayOf()) : this(RouteOverviewConfig(path, roles.toSet()))

    lateinit var renderer: RouteOverviewRenderer

    override fun init(app: Javalin) {
        renderer = RouteOverviewRenderer(app)
    }

    override fun apply(app: Javalin) {
        app.get(this.config.path, renderer, *this.config.roles.toTypedArray())
    }
}
