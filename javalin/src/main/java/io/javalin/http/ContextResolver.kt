package io.javalin.http

import io.javalin.core.util.Header
import javax.servlet.http.HttpServletRequest


const val CONTEXT_RESOLVER_KEY = "contextResolver"

fun Context.contextResolver() = this.appAttribute<ContextResolver>(CONTEXT_RESOLVER_KEY)

class ContextResolver {
    var ip: (Context) -> String = { it.req.remoteAddr }
    var host: (Context) -> String? = { it.header(Header.HOST) }
    var scheme: (Context) -> String = { it.req.scheme }
    var url: (Context) -> String = { it.req.requestURL.toString() }
    var fullUrl: (Context) -> String = { it.url() + if (it.queryString() != null) "?" + it.queryString() else "" }
}
