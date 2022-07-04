package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.util.Header.X_FORWARDED_PROTO
import io.javalin.http.HttpCode.MOVED_PERMANENTLY
import io.javalin.http.util.ContextUtil.isLocalhost

/**
 * [SslRedirectPlugin] has to be the first registered plugin to properly handle all requests in 'before' handler.
 */
class SslRedirectPlugin : Plugin {

    override fun apply(app: Javalin) {
        app.before { ctx ->
            if (ctx.isLocalhost()) {
                return@before
            }

            val xForwardedProto = ctx.header(X_FORWARDED_PROTO)

            if (xForwardedProto == "http" || (xForwardedProto == null && ctx.scheme() == "http")) {
                ctx.redirect(ctx.fullUrl().replace("http", "https"), MOVED_PERMANENTLY.status)
            }
        }
    }

}
