/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.MOVED_PERMANENTLY
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginPriority
import io.javalin.router.matcher.PathParser
import io.javalin.router.matcher.PathSegment
import io.javalin.util.Util.firstOrNull
import java.util.*

/**
 * This plugin redirects requests with uppercase/mixcase paths to lowercase paths
 * Ex: `/Users/John` -> `/users/John` (if endpoint is `/users/{userId}`)
 * It does not affect the casing of path-params and query-params, only static
 * URL fragments ('Users' becomes 'users' above, but 'John' remains 'John').
 * When using this plugin, you can only add paths with lowercase URL fragments.
 */
open class RedirectToLowercasePathPlugin : Plugin<Void>() {

    override fun onInitialize(config: JavalinConfig) {
        if (config.router.caseInsensitiveRoutes) {
            throw IllegalStateException("RedirectToLowercasePathPlugin is not compatible with caseInsensitiveRoutes")
        }

        config.events.handlerAdded { handlerMetaInfo ->
            val parser = PathParser(handlerMetaInfo.path, config.router)

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

    override fun onStart(config: JavalinConfig) {
        config.routes.before { ctx ->
            val requestUri = ctx.path().removePrefix(ctx.contextPath())
            val router = config.pvt.internalRouter

            if (router.findHttpHandlerEntries(ctx.method(), requestUri).findFirst().isPresent) {
                return@before // we found a route for this case, no need to redirect
            }

            val lowercaseRoute = router.findHttpHandlerEntries(ctx.method(), requestUri.lowercase(Locale.ROOT))
                .firstOrNull()
                ?: return@before // lowercase route not found

            val clientSegments = requestUri.split("/")
                .filter { it.isNotEmpty() }
                .toTypedArray()

            val serverSegments = PathParser(lowercaseRoute.endpoint.path, config.router)
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
                status = MOVED_PERMANENTLY
            )
        }
    }

    override fun priority(): PluginPriority = PluginPriority.EARLY

}
