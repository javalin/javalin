/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.http.HandlerType
import io.javalin.http.HttpCode.MOVED_PERMANENTLY
import io.javalin.routing.PathParser
import io.javalin.routing.PathSegment
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
        app.events { listener ->
            listener.handlerAdded { handlerMetaInfo ->
                val parser = PathParser(handlerMetaInfo.path, app.cfg.routing)

                parser.segments.asSequence()
                    .filterIsInstance<PathSegment.Normal>()
                    .map { it.content }
                    .firstOrNull { it != it.lowercase(Locale.ROOT) }
                    ?.run { throw IllegalArgumentException("Paths must be lowercase when using RedirectToLowercasePathPlugin") }

                parser.segments.asSequence()
                    .filterIsInstance<PathSegment.MultipleSegments>()
                    .flatMap { it.innerSegments }
                    .filterIsInstance<PathSegment.Normal>()
                    .map { it.content }
                    .firstOrNull { it != it.lowercase(Locale.ROOT) }
                    ?.run { throw IllegalArgumentException("Paths must be lowercase when using RedirectToLowercasePathPlugin") }
            }
        }
    }

    override fun apply(app: Javalin) {
        app.before { ctx ->
            val requestUri = ctx.path().removePrefix(ctx.contextPath())
            val matcher = app.javalinServlet().matcher

            if (matcher.findEntries(ctx.method(), requestUri).firstOrNull() != null) {
                return@before // we found a route for this case, no need to redirect
            }

            val lowercaseRoute = matcher.findEntries(ctx.method(), requestUri.lowercase(Locale.ROOT))
                .firstOrNull()
                ?: return@before // lowercase route not found

            val clientSegments = requestUri.split("/")
                .filter { it.isNotEmpty() }
                .toTypedArray()

            val serverSegments = PathParser(lowercaseRoute.path, app.cfg.routing)
                .segments

            serverSegments.forEachIndexed { index, serverSegment ->
                // this is also a "Normal" segment
                if (serverSegment is PathSegment.Normal) {
                    clientSegments[index] = clientSegments[index].lowercase(Locale.ROOT)
                }

                // replace the non lowercased part of the segment with the lowercased version
                if (serverSegment is PathSegment.MultipleSegments) {
                    serverSegment.innerSegments
                        .filterIsInstance<PathSegment.Normal>()
                        .forEach { innerServerSegment ->
                            clientSegments[index] = clientSegments[index].replace(
                                innerServerSegment.content,
                                innerServerSegment.content.lowercase(Locale.ROOT),
                                ignoreCase = true
                            )
                        }
                }
            }

            ctx.redirect(
                location = "/" + clientSegments.joinToString("/") + (ctx.queryString()?.let { "?$it" } ?: ""), // lowercase path
                httpCode = MOVED_PERMANENTLY
            )
        }
    }

}

