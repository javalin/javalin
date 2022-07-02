package io.javalin.core.config

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
import java.util.function.Supplier

class DefaultPluginConfig(private val c: JavalinConfig) {
    fun enableCorsForAllOrigins() = c.registerPlugin(CorsPlugin.forAllOrigins())
    fun enableCorsForOrigin(vararg origins: String) = c.registerPlugin(CorsPlugin.forOrigins(*origins))
    fun enableHttpAllowedMethodsOnRoutes() = c.registerPlugin(HttpAllowedMethodsOnRoutesUtil())
    fun enableDevLogging() = c.registerPlugin(DevLoggingPlugin())
    fun enableGlobalHeaders(headers: Supplier<Headers?>) = c.registerPlugin(HeadersPlugin(headers.get()!!))
    fun enableRouteOverview(routeOverviewConfig: RouteOverviewConfig) = c.registerPlugin(RouteOverviewPlugin(routeOverviewConfig))
    fun enableRouteOverview(path: String, vararg roles: RouteRole = arrayOf()) = enableRouteOverview(RouteOverviewConfig(path, roles.toSet()))
    fun enableRedirectToLowercasePaths() = c.registerPlugin(RedirectToLowercasePathPlugin())
    fun enableBasicAuth(username: String, password: String) = c.registerPlugin(BasicAuthFilter(username, password))

}
