/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.core.routing.PathMatcher
import io.javalin.http.Context

object LogUtil {

    fun setup(ctx: Context, matcher: PathMatcher, hasRequestLogger: Boolean) {
        if (!hasRequestLogger) return
        ctx.attribute("javalin-request-log-matcher", matcher)
        ctx.attribute("javalin-request-log-start-time", System.nanoTime())
    }

    fun executionTimeMs(ctx: Context) = (System.nanoTime() - ctx.attribute<Long>("javalin-request-log-start-time")!!) / 1000000f

}

