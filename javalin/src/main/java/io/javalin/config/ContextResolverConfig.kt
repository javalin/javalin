package io.javalin.config

import io.javalin.http.Context
import io.javalin.http.Header

/**
 * Configure the implementation for Context functions.
 *
 * @see [JavalinState.contextResolver]
 */
class ContextResolverConfig {
    companion object {
        internal val ContextResolverKey = Key<ContextResolverConfig>("javalin-context-resolver")
    }
    // @formatter:off
    /** The IP address resolver (default: reads the `remoteAddr` part of the request) */
    @JvmField var ip: (Context) -> String = { it.req().remoteAddr }
    /** The host resolver (default: reads the `Host` header). */
    @JvmField var host: (Context) -> String? = { it.header(Header.HOST) }
    /** The scheme resolver (default: reads the `scheme` part of the request). */
    @JvmField var scheme: (Context) -> String = { it.req().scheme }
    /** The URL resolver (default: reads the `requestUrl` part of the request). */
    @JvmField var url: (Context) -> String = { it.req().requestURL.toString() }
    /** The full URL resolver (default: reads the url with its query string). */
    @JvmField var fullUrl: (Context) -> String = { it.url() + if (it.queryString() != null) "?" + it.queryString() else "" }
    // @formatter:on
}
