package io.javalin.config

import io.javalin.plugin.bundled.BasicAuthPlugin
import io.javalin.plugin.bundled.CorsPluginConfig
import io.javalin.plugin.bundled.CorsPlugin
import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.plugin.bundled.DevReloadPlugin
import io.javalin.plugin.bundled.GlobalHeadersConfig
import io.javalin.plugin.bundled.GlobalHeadersPlugin
import io.javalin.plugin.bundled.HttpAllowedMethodsPlugin
import io.javalin.plugin.bundled.RedirectToLowercasePathPlugin
import io.javalin.plugin.bundled.RouteOverviewPlugin
import io.javalin.plugin.bundled.SslRedirectPlugin
import io.javalin.security.RouteRole
import java.util.function.Consumer

/**
 * Configuration to enable bundled plugins or add custom ones.
 *
 * @see [JavalinConfig.bundledPlugins]
 */
class BundledPluginsConfig(private val cfg: JavalinConfig) {

    /**
     * Enables the RouteOverview plugin.
     * @param path the path from which the route overview should be visible
     * @param roles the roles restricting who can access the route overview
     * @see [RouteOverviewPlugin]
     */
    fun enableRouteOverview(path: String, vararg roles: RouteRole = emptyArray()) =
        cfg.registerPlugin(RouteOverviewPlugin {
            it.path = path
            it.roles = roles
        })

    /**
     * Enables the Basic Authentication plugin.
     * @see [BasicAuthPlugin]
     */
    fun enableBasicAuth(username: String, password: String) =
        cfg.registerPlugin(BasicAuthPlugin {
            it.username = username
            it.password = password
        })

    /**
     * Enables the Global Headers plugin.
     * @see [GlobalHeadersPlugin]
     */
    fun enableGlobalHeaders(globalHeadersConfig: Consumer<GlobalHeadersConfig>) = cfg.registerPlugin(GlobalHeadersPlugin(globalHeadersConfig))

    /**
     * Enables the Cors Plugin.
     * @see [GlobalHeadersPlugin]
     */
    fun enableCors(userConfig: Consumer<CorsPluginConfig>) = cfg.registerPlugin(CorsPlugin(userConfig))
    /** Enables the HttpAllowedMethodsPlugin, automatically handling the Options request on configured routes. */
    fun enableHttpAllowedMethodsOnRoutes() = cfg.registerPlugin(HttpAllowedMethodsPlugin())
    /**  Enables the development debugging logger plugin. */
    fun enableDevLogging() = cfg.registerPlugin(DevLoggingPlugin())
    /**  Enables the development debugging logger plugin with the specified config */
    fun enableDevLogging(userConfig: Consumer<DevLoggingPlugin.Config>) = cfg.registerPlugin(DevLoggingPlugin(userConfig))
    /** Enables the dev reload plugin for automatic server reload during development. */
    fun enableDevReload(userConfig: Consumer<DevReloadPlugin.Config>) = cfg.registerPlugin(DevReloadPlugin(userConfig))
    /** Enables the RedirectToLowercasePath plugin. */
    fun enableRedirectToLowercasePaths() = cfg.registerPlugin(RedirectToLowercasePathPlugin())
    /** Enables the SSL Redirects plugin, which redirect any http request to https. Note it must be the first plugin enabled to properly handle all requests.*/
    fun enableSslRedirects() = cfg.registerPlugin(SslRedirectPlugin())

}
