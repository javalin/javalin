package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.security.Role

class RouteOverviewPlugin(val config: RouteOverviewConfig) : Plugin, PluginLifecycleInit {
    @JvmOverloads
    constructor(path: String, roles: Set<Role> = setOf()) : this(RouteOverviewConfig(path, roles))

    lateinit var renderer: RouteOverviewRenderer

    override fun init(app: Javalin) {
        renderer = RouteOverviewRenderer(app)
    }

    override fun apply(app: Javalin) {
        app.get(this.config.path, renderer, this.config.roles)
    }
}
