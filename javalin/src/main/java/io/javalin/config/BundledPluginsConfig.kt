package io.javalin.config

import io.javalin.plugin.bundled.BasicAuthPlugin.Companion.BasicAuthPlugin
import io.javalin.plugin.bundled.CorsPlugin.Companion.CorsPlugin
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.plugin.bundled.DevLoggingPlugin.Companion.DevLoggingPlugin
import io.javalin.plugin.bundled.GlobalHeaderConfig
import io.javalin.plugin.bundled.GlobalHeadersPlugin.Companion.GlobalHeadersPlugin
import io.javalin.plugin.bundled.HttpAllowedMethodsPlugin.Companion.HttpAllowedMethodsPlugin
import io.javalin.plugin.bundled.RedirectToLowercasePathPlugin.Companion.RedirectToLowercasePathPlugin
import io.javalin.plugin.bundled.RouteOverviewPlugin.Companion.RouteOverviewPlugin
import io.javalin.plugin.bundled.SslRedirectPlugin.Companion.SslRedirectPlugin
import io.javalin.security.RouteRole
import java.util.function.Consumer

class BundledPluginsConfig(private val cfg: JavalinConfig) {

    fun enableRouteOverview(path: String, vararg roles: RouteRole = emptyArray()) =
        cfg.registerPlugin(RouteOverviewPlugin) {
            it.path = path
            it.roles = roles
        }

    fun enableBasicAuth(username: String, password: String) =
        cfg.registerPlugin(BasicAuthPlugin) {
            it.username = username
            it.password = password
        }

    fun enableGlobalHeaders(globalHeaderConfig: Consumer<GlobalHeaderConfig>) = cfg.registerPlugin(GlobalHeadersPlugin, globalHeaderConfig)
    fun enableCors(corsConfig: Consumer<CorsPluginConfig>) = cfg.registerPlugin(CorsPlugin, corsConfig)
    fun enableHttpAllowedMethodsOnRoutes() = cfg.registerPlugin(HttpAllowedMethodsPlugin)
    fun enableDevLogging() = cfg.registerPlugin(DevLoggingPlugin)
    fun enableRedirectToLowercasePaths() = cfg.registerPlugin(RedirectToLowercasePathPlugin)
    fun enableSslRedirects() = cfg.registerPlugin(SslRedirectPlugin)

}
