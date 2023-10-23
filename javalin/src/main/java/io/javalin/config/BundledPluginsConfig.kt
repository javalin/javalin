package io.javalin.config

import io.javalin.plugin.bundled.BasicAuthPlugin.Companion.BasicAuthPlugin
import io.javalin.plugin.bundled.BasicAuthPluginConfig
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

    fun enableRouteOverview(path: String, vararg roles: RouteRole = emptyArray()): BundledPluginsConfig = also {
        cfg.registerPlugin(RouteOverviewPlugin) {
            it.path = path
            it.roles = roles
        }
    }

    fun enableBasicAuth(username: String, password: String): BundledPluginsConfig = also {
        cfg.registerPlugin(BasicAuthPlugin) {
            it.username = username
            it.password = password
        }
    }

    fun enableGlobalHeaders(globalHeaderConfig: Consumer<GlobalHeaderConfig>): BundledPluginsConfig = also { cfg.registerPlugin(GlobalHeadersPlugin, globalHeaderConfig) }
    fun enableCors(corsConfig: Consumer<CorsPluginConfig>): BundledPluginsConfig = also { cfg.registerPlugin(CorsPlugin, corsConfig) }
    fun enableHttpAllowedMethodsOnRoutes(): BundledPluginsConfig = also { cfg.registerPlugin(HttpAllowedMethodsPlugin) }
    fun enableDevLogging(): BundledPluginsConfig = also { cfg.registerPlugin(DevLoggingPlugin) }
    fun enableRedirectToLowercasePaths(): BundledPluginsConfig = also { cfg.registerPlugin(RedirectToLowercasePathPlugin) }
    fun enableSslRedirects(): BundledPluginsConfig = also { cfg.registerPlugin(SslRedirectPlugin) }

}
