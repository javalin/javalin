/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.http.Header.WWW_AUTHENTICATE
import io.javalin.http.UnauthorizedResponse
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginConfiguration
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.createUserConfig
import java.util.function.Consumer

class BasicAuthPluginConfig : PluginConfiguration {
    @JvmField var username: String? = null
    @JvmField var password: String? = null
}

/**
 * Adds a filter that runs before every http request.
 * Note: It does not apply to websocket upgrade requests
 */
class BasicAuthPlugin(config: Consumer<BasicAuthPluginConfig>) : JavalinPlugin {

    open class BasicAuth : PluginFactory<BasicAuthPlugin, BasicAuthPluginConfig> {
        override fun create(config: Consumer<BasicAuthPluginConfig>): BasicAuthPlugin = BasicAuthPlugin(config)
    }

    companion object {
        object BasicAuth : BasicAuthPlugin.BasicAuth()
    }

    private val config = config.createUserConfig(BasicAuthPluginConfig())

    override fun onStart(app: Javalin) {
        app.before { ctx ->
            val matched = runCatching { ctx.basicAuthCredentials() }
                .fold(
                    onSuccess = { it?.username == config.username && it?.password == config.password },
                    onFailure = { false }
                )

            if (!matched) {
                ctx.header(WWW_AUTHENTICATE, "Basic")
                throw UnauthorizedResponse()
            }
        }
    }

}
