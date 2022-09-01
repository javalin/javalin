package io.javalin.config

import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginAlreadyRegisteredException
import io.javalin.plugin.bundled.BasicAuthPlugin
import io.javalin.plugin.bundled.CORS_KEY
import io.javalin.plugin.bundled.CorsContainerPlugin
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.plugin.bundled.GlobalHeaderConfig
import io.javalin.plugin.bundled.GlobalHeadersPlugin
import io.javalin.plugin.bundled.HttpAllowedMethodsPlugin
import io.javalin.plugin.bundled.RedirectToLowercasePathPlugin
import io.javalin.plugin.bundled.RouteOverviewPlugin
import io.javalin.plugin.bundled.SslRedirectPlugin
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
    fun enableCors(userConfig: Consumer<CorsPluginConfig>) {
        pvt.appAttributes.putIfAbsent(CORS_KEY, CorsContainerPlugin().also { register(it) })
        (pvt.appAttributes[CORS_KEY] as? CorsContainerPlugin)?.addCors(userConfig)
    }

}
