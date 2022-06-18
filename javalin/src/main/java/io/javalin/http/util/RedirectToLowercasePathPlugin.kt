/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.Javalin
import io.javalin.core.PathParser
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.routing.PathSegment
import io.javalin.http.HandlerType
import java.util.*

/**
 * This plugin redirects requests with uppercase/mixcase paths to lowercase paths
 * Ex: `/Users/John` -> `/users/John` (if endpoint is `/users/{userId}`)
 * It does not affect the casing of path-params and query-params, only static
 * URL fragments ('Users' becomes 'users' above, but 'John' remains 'John').
 * When using this plugin, you can only add paths with lowercase URL fragments.
 */
class RedirectToLowercasePathPlugin : Plugin, PluginLifecycleInit {

    override fun init(app: Javalin) {
        app.events { e ->
            e.handlerAdded { h ->
                val parser = PathParser(h.path, app._conf)
                parser.segments.filterIsInstance<PathSegment.Normal>().map { it.content }.forEach {
                    if (it != it.lowercase(Locale.ROOT)) throw IllegalArgumentException("Paths must be lowercase when using RedirectToLowercasePathPlugin")
                }
                parser.segments
                    .filterIsInstance<PathSegment.MultipleSegments>()
                    .flatMap { it.innerSegments }
                    .filterIsInstance<PathSegment.Normal>()
                    .map { it.content }
                    .forEach {
                        if (it != it.lowercase(Locale.ROOT)) {
                            throw IllegalArgumentException("Paths must be lowercase when using RedirectToLowercasePathPlugin")
                        }
                    }
            }
        }
    }

    override fun apply(app: Javalin) {
        app.before { ctx ->
            val type = HandlerType.fromServletRequest(ctx.req)
            val requestUri = ctx.req.requestURI.removePrefix(ctx.req.contextPath)
            val matcher = app.javalinServlet().matcher
            matcher.findEntries(type, requestUri).firstOrNull()?.let {
                return@before // we found a route for this case, no need to redirect
            }
            matcher.findEntries(type, requestUri.lowercase(Locale.ROOT)).firstOrNull()?.let { entry ->
                val clientSegments = requestUri.split("/").filter { it.isNotEmpty() }.toTypedArray()
                val serverSegments = PathParser(entry.path, app._conf).segments
                serverSegments.forEachIndexed { i, serverSegment ->
                    if (serverSegment is PathSegment.Normal) {
                        clientSegments[i] = clientSegments[i].lowercase(Locale.ROOT) // this is also a "Normal" segment
                    }
                    if (serverSegment is PathSegment.MultipleSegments) {
                        serverSegments.forEach { innerServerSegment ->
                            if (innerServerSegment is PathSegment.Normal) {
                                // replace the non lowercased part of the segment with the lowercased version
                                clientSegments[i] = clientSegments[i].replace(
                                    innerServerSegment.content,
                                    innerServerSegment.content.lowercase(Locale.ROOT),
                                    ignoreCase = true
                                )
                            }
                        }
                    }
                }
                val lowercasePath =
                    "/" + clientSegments.joinToString("/") + if (ctx.queryString() != null) "?" + ctx.queryString() else ""
                ctx.redirect(lowercasePath, 301)
            }
        }
    }

}

