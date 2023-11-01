package io.javalin.config

import io.javalin.plugin.bundled.BasicAuthPlugin
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.plugin.bundled.CorsPlugin
import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.plugin.bundled.GlobalHeadersConfig
import io.javalin.plugin.bundled.GlobalHeadersPlugin
import io.javalin.plugin.bundled.HttpAllowedMethodsPlugin
import io.javalin.plugin.bundled.RedirectToLowercasePathPlugin
import io.javalin.plugin.bundled.RouteOverviewPlugin
import io.javalin.plugin.bundled.SslRedirectPlugin
import io.javalin.security.RouteRole
import java.util.function.Consumer

class BundledPluginsConfig(private val cfg: JavalinConfig) {

    fun enableRouteOverview(path: String, vararg roles: RouteRole = emptyArray()) =
        cfg.registerPlugin(RouteOverviewPlugin {
            it.path = path
            it.roles = roles
        })

    fun enableBasicAuth(username: String, password: String) =
        cfg.registerPlugin(BasicAuthPlugin {
            it.username = username
            it.password = password
        })

    fun enableGlobalHeaders(globalHeadersConfig: Consumer<GlobalHeadersConfig>) = cfg.registerPlugin(GlobalHeadersPlugin(globalHeadersConfig))
    fun enableCors(userConfig: Consumer<CorsPluginConfig>) = cfg.registerPlugin(CorsPlugin(userConfig))
    fun enableHttpAllowedMethodsOnRoutes() = cfg.registerPlugin(HttpAllowedMethodsPlugin())
    fun enableDevLogging() = cfg.registerPlugin(DevLoggingPlugin())
    fun enableRedirectToLowercasePaths() = cfg.registerPlugin(RedirectToLowercasePathPlugin())
    fun enableSslRedirects() = cfg.registerPlugin(SslRedirectPlugin())

}
