package io.javalin.plugin.bundled

import io.javalin.config.JavalinState
import io.javalin.http.ExceptionHandler
import io.javalin.http.HttpResponseException
import io.javalin.router.InternalRouter
import io.javalin.router.exception.HttpResponseExceptionMapper

/**
 * Reflection-based helper that clears all route/handler state from a running Javalin instance.
 * This allows the [DevReloadPlugin] to reset routes without requiring any `clear()` methods
 * on core framework classes — keeping the core framework unchanged.
 *
 * All accessed fields exist on stable internal classes and are covered by DevReloadPlugin tests.
 */
@Suppress("UNCHECKED_CAST")
internal object DevReloadReflection {

    /**
     * Clears all HTTP routes, error handlers, exception handlers, and WebSocket handlers
     * from the given [state], preparing it for a fresh config consumer execution.
     */
    fun resetAllRoutes(state: JavalinState) {
        val router = state.internalRouter

        // --- HTTP ---

        // Clear HTTP path matcher entries (InternalRouter.httpPathMatcher -> PathMatcher.handlerEntries)
        val pathMatcher = getField(router, InternalRouter::class.java, "httpPathMatcher")
        val handlerEntries = getField(pathMatcher, pathMatcher.javaClass, "handlerEntries") as MutableMap<*, MutableList<*>>
        handlerEntries.values.forEach { it.clear() }

        // Clear error handlers (InternalRouter.httpErrorMapper -> ErrorMapper.errorHandlers)
        val errorMapper = getField(router, InternalRouter::class.java, "httpErrorMapper")
        val errorHandlers = getField(errorMapper, errorMapper.javaClass, "errorHandlers") as MutableCollection<*>
        errorHandlers.clear()

        // Clear and reset exception handlers (InternalRouter.httpExceptionMapper.handlers is public)
        val exceptionMapper = getField(router, InternalRouter::class.java, "httpExceptionMapper")
        val exceptionHandlers = exceptionMapper.javaClass.getMethod("getHandlers").invoke(exceptionMapper) as MutableMap<Class<out Exception>, ExceptionHandler<Exception>?>
        exceptionHandlers.clear()
        exceptionHandlers[HttpResponseException::class.java] = ExceptionHandler { e, ctx ->
            HttpResponseExceptionMapper.handle(e as HttpResponseException, ctx)
        }

        // --- WebSocket ---

        // Get WsRouter from InternalRouter (private constructor param)
        val wsRouter = getField(router, InternalRouter::class.java, "wsRouter")

        // WsRouter.wsPathMatcher is public, but WsPathMatcher.wsHandlerEntries is private
        val wsPathMatcher = wsRouter.javaClass.getMethod("getWsPathMatcher").invoke(wsRouter)
        val wsHandlerEntries = getField(wsPathMatcher, wsPathMatcher.javaClass, "wsHandlerEntries") as MutableMap<*, MutableList<*>>
        wsHandlerEntries.values.forEach { it.clear() }

        // WsRouter.wsExceptionMapper is public, and WsExceptionMapper.handlers is public
        val wsExceptionMapper = wsRouter.javaClass.getMethod("getWsExceptionMapper").invoke(wsRouter)
        val wsExHandlers = wsExceptionMapper.javaClass.getMethod("getHandlers").invoke(wsExceptionMapper) as MutableMap<*, *>
        wsExHandlers.clear()
    }

    /**
     * Temporarily suppresses duplicate plugin registration during config consumer re-execution.
     * Swaps the PluginManager's internal `plugins` list with a guarded wrapper that silently
     * ignores add() for already-registered plugin types, then restores it afterward.
     */
    fun <T> withPluginReloadingEnabled(state: JavalinState, block: () -> T): T {
        val pluginManager = state.pluginManager
        val pluginsField = pluginManager.javaClass.getDeclaredField("plugins")
        pluginsField.isAccessible = true

        val originalPlugins = pluginsField.get(pluginManager) as MutableList<Any>
        val existingClasses = originalPlugins.map { it.javaClass }.toSet()

        // Replace with a wrapper that silently ignores add() for already-registered plugin types
        val guardedList = object : ArrayList<Any>(originalPlugins) {
            override fun add(element: Any): Boolean {
                if (element.javaClass in existingClasses) return false // skip duplicate
                return super.add(element)
            }
        }
        pluginsField.set(pluginManager, guardedList)

        try {
            return block()
        } finally {
            pluginsField.set(pluginManager, originalPlugins)
        }
    }

    private fun getField(target: Any, clazz: Class<*>, fieldName: String): Any {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target)
    }
}
