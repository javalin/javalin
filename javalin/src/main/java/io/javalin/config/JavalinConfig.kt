/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.http.servlet.MAX_REQUEST_SIZE_KEY
import io.javalin.json.JSON_MAPPER_KEY
import io.javalin.json.JavalinJackson
import io.javalin.json.JsonMapper
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginConfiguration
import io.javalin.plugin.PluginFactory
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
import io.javalin.rendering.FILE_RENDERER_KEY
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.NotImplementedRenderer
import io.javalin.security.AccessManager
import io.javalin.security.RouteRole
import io.javalin.validation.JavalinValidation.addValidationExceptionMapper
import io.javalin.vue.JAVALINVUE_CONFIG_KEY
import io.javalin.vue.JavalinVueConfig
import java.util.function.Consumer

// this class should be abbreviated `cfg` in the source code.
// `cfg.pvt` should be accessible, but usage should be discouraged (hence the naming)
class JavalinConfig {
    //@formatter:off
    @JvmField val pvt = PrivateConfig() // this is "private", only use it if you know what you're doing
    @JvmField val http = HttpConfig()
    @JvmField val routing = RoutingConfig()
    @JvmField val jetty = JettyConfig()
    @JvmField val staticFiles = StaticFilesConfig(pvt)
    @JvmField val spaRoot = SpaRootConfig(pvt)
    @JvmField val compression = CompressionConfig(pvt)
    @JvmField val requestLogger = RequestLoggerConfig(pvt)
    @JvmField val plugins = PluginConfig()
    @JvmField val events = EventConfig()
    @JvmField val vue = JavalinVueConfig()
    @JvmField val contextResolver = ContextResolverConfig()
    @JvmField var showJavalinBanner = true
    fun accessManager(accessManager: AccessManager) { pvt.accessManager = accessManager }
    fun jsonMapper(jsonMapper: JsonMapper) { pvt.appAttributes[JSON_MAPPER_KEY] = jsonMapper }
    fun fileRenderer(fileRenderer: FileRenderer) { pvt.appAttributes[FILE_RENDERER_KEY] = fileRenderer }
    //@formatter:on

    companion object {
        @JvmStatic
        fun applyUserConfig(app: Javalin, cfg: JavalinConfig, userConfig: Consumer<JavalinConfig>) {
            addValidationExceptionMapper(app) // add default mapper for validation
            userConfig.accept(cfg) // apply user config to the default config
            cfg.plugins.pluginManager.initializePlugins(app)
            cfg.pvt.appAttributes.computeIfAbsent(JSON_MAPPER_KEY) { JavalinJackson() }
            cfg.pvt.appAttributes.computeIfAbsent(FILE_RENDERER_KEY) { NotImplementedRenderer() }
            cfg.pvt.appAttributes.computeIfAbsent(CONTEXT_RESOLVER_KEY) { cfg.contextResolver }
            cfg.pvt.appAttributes.computeIfAbsent(MAX_REQUEST_SIZE_KEY) { cfg.http.maxRequestSize }
            cfg.pvt.appAttributes.computeIfAbsent(JAVALINVUE_CONFIG_KEY) { cfg.vue }
        }
    }

    fun registerPlugin(plugin: JavalinPlugin): JavalinConfig = also {
        plugins.pluginManager.register(plugin)
    }

    @JvmOverloads
    fun <PLUGIN : JavalinPlugin, CFG : PluginConfiguration> registerPlugin(factory: PluginFactory<PLUGIN, CFG>, cfg: Consumer<CFG> = Consumer {}) =
        registerPlugin(factory.create(cfg))

    fun enableRouteOverview(path: String, vararg roles: RouteRole = emptyArray()) =
        registerPlugin(RouteOverview) {
            it.path = path
            it.roles = roles
        }

    fun enableBasicAuth(username: String, password: String) =
        registerPlugin(BasicAuth) {
            it.username = username
            it.password = password
        }

    fun enableGlobalHeaders(globalHeaderConfig: Consumer<GlobalHeaderConfig>) = registerPlugin(GlobalHeaders, globalHeaderConfig)
    fun enableCors(userConfig: Consumer<CorsPluginConfig>) = registerPlugin(Cors, userConfig)
    fun enableHttpAllowedMethodsOnRoutes() = registerPlugin(HttpAllowedMethods)
    fun enableDevLogging() = registerPlugin(DevLogging)
    fun enableRedirectToLowercasePaths() = registerPlugin(RedirectToLowercasePath)
    fun enableSslRedirects() = registerPlugin(SslRedirect)

}
