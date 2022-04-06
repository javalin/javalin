package io.javalin.http

import io.javalin.core.util.Header
import java.io.InputStream


const val CONTEXT_RESOLVER_KEY = "contextResolver"

fun Context.contextResolver() = this.appAttribute<ContextResolver>(CONTEXT_RESOLVER_KEY)

class ContextResolver {
    // @formatter:off
    @JvmField var ip: (Context) -> String = { it.req.remoteAddr }
    @JvmField var host: (Context) -> String? = { it.header(Header.HOST) }
    @JvmField var scheme: (Context) -> String = { it.req.scheme }
    @JvmField var url: (Context) -> String = { it.req.requestURL.toString() }
    @JvmField var fullUrl: (Context) -> String = { it.url() + if (it.queryString() != null) "?" + it.queryString() else "" }
    @JvmField var defaultFutureCallback: (ctx: Context, result: Any?) -> Unit = { ctx, result ->
        when (result) {
            is Unit -> {}
            is InputStream -> ctx.result(result)
            is String -> ctx.result(result)
            is Any -> ctx.json(result)
        }
    }
    // @formatter:on
}
