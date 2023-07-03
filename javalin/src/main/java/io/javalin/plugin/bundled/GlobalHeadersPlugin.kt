/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.plugin.JavalinPlugin

class GlobalHeadersPlugin(private val globalHeaderConfig: GlobalHeaderConfig) : JavalinPlugin {

    override fun onStart(app: Javalin) {
        app.before { ctx ->
            globalHeaderConfig.headers.forEach { (name, value) ->
                ctx.header(name, value)
            }
        }
    }

}
