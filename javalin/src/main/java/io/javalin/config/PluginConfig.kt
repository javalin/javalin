package io.javalin.config

import io.javalin.plugin.BasicAuthFilter
import io.javalin.plugin.CorsPlugin
import io.javalin.plugin.CorsPluginConfig
import io.javalin.plugin.DevLoggingPlugin
import io.javalin.plugin.Headers
import io.javalin.plugin.HeadersPlugin
import io.javalin.plugin.HttpAllowedMethodsOnRoutesUtil
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

    fun enableHttpAllowedMethodsOnRoutes() = register(HttpAllowedMethodsOnRoutesUtil())
    fun enableDevLogging() = register(DevLoggingPlugin())
    fun enableGlobalHeaders(headers: Supplier<Headers?>) = register(HeadersPlugin(headers.get()!!))
    fun enableRouteOverview(path: String, vararg roles: RouteRole = arrayOf()) = register(RouteOverviewPlugin(path, *roles))

    fun enableRedirectToLowercasePaths() = register(RedirectToLowercasePathPlugin())
    fun enableBasicAuth(username: String, password: String) = register(BasicAuthFilter(username, password))
    fun enableSslRedirects() = register(SslRedirectPlugin())
    fun enableCors(userConfig: Consumer<CorsPluginConfig>) = register(CorsPlugin(userConfig))

}
