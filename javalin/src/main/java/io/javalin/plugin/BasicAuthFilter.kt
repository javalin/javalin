/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.util.Header.WWW_AUTHENTICATE
import io.javalin.http.UnauthorizedResponse

/**
 * Adds a filter that runs before every http request.
 * Note: It does not apply to websocket upgrade requests
 */
class BasicAuthFilter(private val username: String, private val password: String) : Plugin {

    override fun apply(app: Javalin) {
        app.before { ctx ->
            val matched = runCatching { ctx.basicAuthCredentials() }
                .fold(
                    onSuccess = { it.username == username && it.password == password },
                    onFailure = { false }
                )

            if (!matched) {
                ctx.header(WWW_AUTHENTICATE, "Basic")
                throw UnauthorizedResponse()
            }
        }
    }

}
