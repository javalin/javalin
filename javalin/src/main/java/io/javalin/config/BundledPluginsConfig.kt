package io.javalin.config

import io.javalin.plugin.bundled.BasicAuthPlugin.Companion.BasicAuth
import io.javalin.plugin.bundled.CorsPlugin.Companion.Cors
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.plugin.bundled.DevLoggingPlugin.Companion.DevLogging
import io.javalin.plugin.bundled.GlobalHeaderConfig
import io.javalin.plugin.bundled.GlobalHeadersPlugin.Companion.GlobalHeaders
import io.javalin.plugin.bundled.HttpAllowedMethodsPlugin.Companion.HttpAllowedMethods
import io.javalin.plugin.bundled.RedirectToLowercasePathPlugin.Companion.RedirectToLowercasePath
import io.javalin.plugin.bundled.RouteOverviewPlugin.Companion.RouteOverview
import io.javalin.plugin.bundled.SslRedirectPlugin.Companion.SslRedirect
import io.javalin.security.RouteRole
import java.util.function.Consumer

class BundledPluginsConfig(private val cfg: JavalinConfig) {

    fun enableRouteOverview(path: String, vararg roles: RouteRole = emptyArray()) =
        cfg.registerPlugin(RouteOverview) {
            it.path = path
            it.roles = roles
        }

    fun enableBasicAuth(username: String, password: String) =
        cfg.registerPlugin(BasicAuth) {
            it.username = username
            it.password = password
        }

    fun enableGlobalHeaders(globalHeaderConfig: Consumer<GlobalHeaderConfig>) = cfg.registerPlugin(GlobalHeaders, globalHeaderConfig)
    fun enableCors(corsConfig: Consumer<CorsPluginConfig>) = cfg.registerPlugin(Cors, corsConfig)
    fun enableHttpAllowedMethodsOnRoutes() = cfg.registerPlugin(HttpAllowedMethods)
    fun enableDevLogging() = cfg.registerPlugin(DevLogging)
    fun enableRedirectToLowercasePaths() = cfg.registerPlugin(RedirectToLowercasePath)
    fun enableSslRedirects() = cfg.registerPlugin(SslRedirect)

}
