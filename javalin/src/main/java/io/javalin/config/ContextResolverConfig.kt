package io.javalin.config

import io.javalin.http.Context
import io.javalin.http.Header

const val CONTEXT_RESOLVER_KEY = "javalin-context-resolver"

fun Context.contextResolver() = this.appAttribute<ContextResolverConfig>(CONTEXT_RESOLVER_KEY)

class ContextResolverConfig {
    // @formatter:off
    @JvmField var ip: (Context) -> String = { it.req().remoteAddr }
    @JvmField var host: (Context) -> String? = { it.header(Header.HOST) }
    @JvmField var scheme: (Context) -> String = { it.req().scheme }
    @JvmField var url: (Context) -> String = { it.req().requestURL.toString() }
    @JvmField var fullUrl: (Context) -> String = { it.url() + if (it.queryString() != null) "?" + it.queryString() else "" }
    // @formatter:on
}
