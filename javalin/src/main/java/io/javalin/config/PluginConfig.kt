package io.javalin.config

import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginConfiguration
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.PluginManager
import io.javalin.plugin.bundled.BasicAuthPlugin.Companion.BasicAuth
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.plugin.bundled.CorsPluginFactory
import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.plugin.bundled.GlobalHeaderConfig
import io.javalin.plugin.bundled.GlobalHeadersPluginFactory
import io.javalin.plugin.bundled.HttpAllowedMethodsPlugin
import io.javalin.plugin.bundled.RedirectToLowercasePathPlugin
import io.javalin.plugin.bundled.RouteOverviewPluginFactory
import io.javalin.plugin.bundled.SslRedirectPlugin
import io.javalin.security.RouteRole
import java.util.function.Consumer

class PluginConfig {

    val pluginManager = PluginManager()

    fun register(plugin: JavalinPlugin): PluginConfig =
        also { pluginManager.register(plugin) }

    @JvmOverloads
    fun <PLUGIN : JavalinPlugin, CFG : PluginConfiguration> register(factory: PluginFactory<PLUGIN, CFG>, cfg: Consumer<CFG> = Consumer {}) =
        register(factory.create(cfg))

    fun enableRouteOverview(path: String, vararg roles: RouteRole = emptyArray()) =
        register(RouteOverviewPluginFactory) {
            it.path = path
            it.roles = roles
        }

    fun enableBasicAuth(username: String, password: String) =
        register(BasicAuth) {
            it.username = username
            it.password = password
        }

    fun enableGlobalHeaders(globalHeaderConfig: Consumer<GlobalHeaderConfig>) =
        register(GlobalHeadersPluginFactory, globalHeaderConfig)

    fun enableCors(userConfig: Consumer<CorsPluginConfig>) =
        register(CorsPluginFactory, userConfig)

    fun enableHttpAllowedMethodsOnRoutes() =
        register(HttpAllowedMethodsPlugin())

    fun enableDevLogging() =
        register(DevLoggingPlugin())

    fun enableRedirectToLowercasePaths() =
        register(RedirectToLowercasePathPlugin())

    fun enableSslRedirects() =
        register(SslRedirectPlugin())

}
