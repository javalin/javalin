package io.javalin.http

import io.javalin.core.util.Header
import javax.servlet.http.HttpServletRequest


const val CONTEXT_RESOLVER_KEY = "contextResolver"

fun Context.contextResolver() = this.appAttribute<ContextResolver>(CONTEXT_RESOLVER_KEY)

class ContextResolver {
    var ip: (Context) -> String = { it.req.remoteAddr }
    var host: (Context) -> String? = { it.header(Header.HOST) }
}
