package io.javalin.core.plugin

import io.javalin.Javalin

/** Extend [Plugin] with a new lifecycle */
interface PluginLifecycleInit {
    /**
     * Initialize properties and event listener.
     * This will be called before any handler is registered.
     * It is not allowed to register handler during this lifecycle.
     */
    fun init(app: Javalin)
}
