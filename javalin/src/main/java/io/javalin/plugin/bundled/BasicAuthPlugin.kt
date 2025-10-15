/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.Header.WWW_AUTHENTICATE
import io.javalin.http.UnauthorizedResponse
import io.javalin.plugin.Plugin
import java.util.function.Consumer

/**
 * Adds a filter that runs before every http request.
 * Note: It does not apply to websocket upgrade requests
 */
class BasicAuthPlugin(userConfig: Consumer<Config>) : Plugin<BasicAuthPlugin.Config>(userConfig, Config()) {

    class Config {
        @JvmField var username: String? = null
        @JvmField var password: String? = null
    }

    override fun onStart(config: JavalinConfig) {
        config.routes.before { ctx ->
            val matched = runCatching { ctx.basicAuthCredentials() }
                .fold(
                    onSuccess = { auth -> auth?.username == pluginConfig.username && auth?.password == pluginConfig.password },
                    onFailure = { false }
                )

            if (!matched) {
                ctx.header(WWW_AUTHENTICATE, "Basic")
                throw UnauthorizedResponse()
            }
        }
    }

}
