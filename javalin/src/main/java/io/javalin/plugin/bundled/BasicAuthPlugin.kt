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
import io.javalin.plugin.PluginFactory
import java.util.function.Consumer

object BasicAuthPluginFactory : PluginFactory<BasicAuthPlugin, BasicAuthPluginConfiguration> {
    override fun create(config: Consumer<BasicAuthPluginConfiguration>): BasicAuthPlugin {
        return BasicAuthPlugin(config)
    }
}

class BasicAuthPluginConfiguration {
    @JvmField var username: String? = null
    @JvmField var password: String? = null
}

/**
 * Adds a filter that runs before every http request.
 * Note: It does not apply to websocket upgrade requests
 */
class BasicAuthPlugin(config: Consumer<BasicAuthPluginConfiguration>) : JavalinPlugin {

    companion object {
        @JvmStatic val FACTORY = BasicAuthPluginFactory
    }

    private val config = BasicAuthPluginConfiguration().apply { config.accept(this) }

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
