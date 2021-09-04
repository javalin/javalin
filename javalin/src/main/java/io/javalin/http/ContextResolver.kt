package io.javalin.http

import io.javalin.core.util.Header
import javax.servlet.http.HttpServletRequest


const val CONTEXT_RESOLVER_KEY = "contextResolver"

fun Context.contextResolver() = this.appAttribute<ContextResolver>(CONTEXT_RESOLVER_KEY)

class ContextResolver {
    var ip = { ctx: Context -> ctx.req.remoteAddr }
    var host = { ctx: Context -> ctx.req.getHeader(Header.HOST) }
}
