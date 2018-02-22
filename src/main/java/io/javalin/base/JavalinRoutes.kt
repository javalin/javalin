package io.javalin.base

import io.javalin.Handler
import io.javalin.Javalin
import io.javalin.core.HandlerEntry
import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher
import io.javalin.core.util.Util

internal abstract class JavalinRoutes : JavalinMappers() {

    protected val pathMatcher = PathMatcher()

    protected fun addHandler(httpMethod: HandlerType, path: String, handler: Handler): Javalin {
        val prefixedPath = Util.prefixContextPath(path, contextPath)
        pathMatcher.handlerEntries.add(HandlerEntry(httpMethod, prefixedPath, handler))
        return this
    }

    // HTTP verbs
    override fun get(path: String, handler: Handler) = addHandler(HandlerType.GET, path, handler)

    override fun post(path: String, handler: Handler) = addHandler(HandlerType.POST, path, handler)

    override fun put(path: String, handler: Handler) = addHandler(HandlerType.PUT, path, handler)

    override fun patch(path: String, handler: Handler) = addHandler(HandlerType.PATCH, path, handler)

    override fun delete(path: String, handler: Handler) = addHandler(HandlerType.DELETE, path, handler)

    override fun head(path: String, handler: Handler) = addHandler(HandlerType.HEAD, path, handler)

    override fun trace(path: String, handler: Handler) = addHandler(HandlerType.TRACE, path, handler)

    override fun connect(path: String, handler: Handler) = addHandler(HandlerType.CONNECT, path, handler)

    override fun options(path: String, handler: Handler) = addHandler(HandlerType.OPTIONS, path, handler)

    // Filters
    override fun before(path: String, handler: Handler) = addHandler(HandlerType.BEFORE, path, handler)

    override fun before(handler: Handler) = before("*", handler)

    override fun after(path: String, handler: Handler) = addHandler(HandlerType.AFTER, path, handler)

    override fun after(handler: Handler) = after("*", handler)

    // Reverse routing
    override fun pathFinder(handler: Handler) = pathMatcher.findHandlerPath { he -> he.handler == handler }

    override fun pathFinder(handler: Handler, type: HandlerType) = pathMatcher.findHandlerPath { he -> he.handler == handler && he.type === type }
}
