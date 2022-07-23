package io.javalin.config

import io.javalin.http.Context
import io.javalin.http.Header
import java.io.InputStream


const val CONTEXT_RESOLVER_KEY = "javalin-context-resolver"

fun Context.contextResolver() = this.appAttribute<ContextResolver>(CONTEXT_RESOLVER_KEY)

class ContextResolver {
    // @formatter:off
    @JvmField var ip: (Context) -> String = { it.request().remoteAddr }
    @JvmField var host: (Context) -> String? = { it.header(Header.HOST) }
    @JvmField var scheme: (Context) -> String = { it.request().scheme }
    @JvmField var url: (Context) -> String = { it.request().requestURL.toString() }
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
