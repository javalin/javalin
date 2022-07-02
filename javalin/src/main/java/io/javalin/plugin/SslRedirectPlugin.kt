package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.http.util.ContextUtil.isLocalhost

// this needs to be the first before-handler
class SslRedirectPlugin : Plugin {
    override fun apply(app: Javalin) {
        app.before { ctx ->
            if (ctx.isLocalhost()) return@before
            val xForwardedProto = ctx.header("x-forwarded-proto")
            if (xForwardedProto == "http" || (xForwardedProto == null && ctx.scheme() == "http")) {
                ctx.redirect(ctx.fullUrl().replace("http", "https"), 301)
            }
        }
    }
}
