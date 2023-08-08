/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.servlet.acceptsHtml
import io.javalin.router.matcher.PathMatcher

object MethodNotAllowedUtil {

    fun findAvailableHttpHandlerTypes(matcher: PathMatcher, requestUri: String) =
        enumValues<HandlerType>().filter { it.isHttpMethod && matcher.findEntries(it, requestUri).findFirst().isPresent }

    fun getAvailableHandlerTypes(ctx: Context, availableHandlerTypes: List<HandlerType>): Map<String, String> = mapOf(
        (if (acceptsHtml(ctx)) "Available methods" else "availableMethods") to availableHandlerTypes.joinToString(", ")
    )
}
