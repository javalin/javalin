package io.javalin.core.config

import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginAlreadyRegisteredException
import io.javalin.core.security.RouteRole
import io.javalin.core.util.Headers
import io.javalin.plugin.BasicAuthFilter
import io.javalin.plugin.CorsPlugin
import io.javalin.plugin.DevLoggingPlugin
import io.javalin.plugin.HeadersPlugin
import io.javalin.plugin.HttpAllowedMethodsOnRoutesUtil
import io.javalin.plugin.RedirectToLowercasePathPlugin
import io.javalin.plugin.RouteOverviewConfig
import io.javalin.plugin.RouteOverviewPlugin
import io.javalin.plugin.SslRedirectPlugin
import java.util.function.Supplier

class PluginConfig(private val pvt: PrivateConfig) {

    fun register(plugin: Plugin) {
        if (pvt.plugins.containsKey(plugin.javaClass)) {
            throw PluginAlreadyRegisteredException(plugin.javaClass)
        }
        pvt.plugins[plugin.javaClass] = plugin
    }

    fun enableCorsForAllOrigins() = register(CorsPlugin.forAllOrigins())
    fun enableCorsForOrigin(vararg origins: String) = register(CorsPlugin.forOrigins(*origins))
    fun enableHttpAllowedMethodsOnRoutes() = register(HttpAllowedMethodsOnRoutesUtil())
    fun enableDevLogging() = register(DevLoggingPlugin())
    fun enableGlobalHeaders(headers: Supplier<Headers?>) = register(HeadersPlugin(headers.get()!!))
    fun enableRouteOverview(routeOverviewConfig: RouteOverviewConfig) = register(RouteOverviewPlugin(routeOverviewConfig))
    fun enableRouteOverview(path: String, vararg roles: RouteRole = arrayOf()) = enableRouteOverview(RouteOverviewConfig(path, roles.toSet()))
    fun enableRedirectToLowercasePaths() = register(RedirectToLowercasePathPlugin())
    fun enableBasicAuth(username: String, password: String) = register(BasicAuthFilter(username, password))
    fun enableSslRedirects() = register(SslRedirectPlugin())

}
