/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.config.JavalinState
import io.javalin.http.Context
import io.javalin.http.Header.WWW_AUTHENTICATE
import io.javalin.http.UnauthorizedResponse
import io.javalin.plugin.Plugin
import java.util.function.Consumer

/**
 * Adds a filter that runs before every http request and every websocket upgrade request.
 */
class BasicAuthPlugin(userConfig: Consumer<Config>) : Plugin<BasicAuthPlugin.Config>(userConfig, Config()) {

    class Config {
        @JvmField var username: String? = null
        @JvmField var password: String? = null
    }

    override fun onStart(state: JavalinState) {
        state.routes.before { ctx -> if (!authorized(ctx)) { ctx.header(WWW_AUTHENTICATE, "Basic"); throw UnauthorizedResponse() } }
        state.routes.wsBeforeUpgrade { ctx -> if (!authorized(ctx)) { ctx.header(WWW_AUTHENTICATE, "Basic"); throw UnauthorizedResponse() } }
    }

    private fun authorized(ctx: Context): Boolean {
        val auth = runCatching { ctx.basicAuthCredentials() }.getOrNull() ?: return false
        val username = pluginConfig.username ?: return false // no configured credentials: nothing can match, fail closed
        val password = pluginConfig.password ?: return false
        return auth.username == username && auth.password == password
    }

}
