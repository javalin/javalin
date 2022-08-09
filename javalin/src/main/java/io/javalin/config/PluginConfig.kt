package io.javalin.config

import io.javalin.plugin.BasicAuthPlugin
import io.javalin.plugin.CorsPlugin
import io.javalin.plugin.CorsPluginConfig
import io.javalin.plugin.DevLoggingPlugin
import io.javalin.plugin.GlobalHeaderConfig
import io.javalin.plugin.GlobalHeadersPlugin
import io.javalin.plugin.HttpAllowedMethodsPlugin
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginAlreadyRegisteredException
import io.javalin.plugin.RedirectToLowercasePathPlugin
import io.javalin.plugin.SslRedirectPlugin
import io.javalin.plugin.routeoverview.RouteOverviewPlugin
import io.javalin.security.RouteRole
import java.util.function.Consumer
import java.util.function.Supplier

class PluginConfig(private val pvt: PrivateConfig) {

    fun register(plugin: Plugin) {
        if (pvt.plugins.containsKey(plugin.javaClass)) {
            throw PluginAlreadyRegisteredException(plugin.javaClass)
        }
        pvt.plugins[plugin.javaClass] = plugin
    }

    fun enableHttpAllowedMethodsOnRoutes() = register(HttpAllowedMethodsPlugin())
    fun enableDevLogging() = register(DevLoggingPlugin())
    fun enableGlobalHeaders(globalHeaderConfig: Supplier<GlobalHeaderConfig?>) = register(GlobalHeadersPlugin(globalHeaderConfig.get()!!))
    fun enableRouteOverview(path: String, vararg roles: RouteRole = arrayOf()) = register(RouteOverviewPlugin(path, *roles))

    fun enableRedirectToLowercasePaths() = register(RedirectToLowercasePathPlugin())
    fun enableBasicAuth(username: String, password: String) = register(BasicAuthPlugin(username, password))
    fun enableSslRedirects() = register(SslRedirectPlugin())
    fun enableCors(userConfig: Consumer<CorsPluginConfig>) = register(CorsPlugin(userConfig))

}
