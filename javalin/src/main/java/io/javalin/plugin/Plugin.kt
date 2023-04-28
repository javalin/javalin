package io.javalin.plugin

import io.javalin.Javalin

/**
 * A extension is a modular way of adding functionality to a Javalin instance.
 * Lifecycle interfaces can be used to listen to specific callbacks.
 * To apply a plugin use [JavalinConfig.registerPlugin].
 */
fun interface Plugin {
    fun apply(app: Javalin)
}

/**
 * A repeatable plugin is a plugin that can be registered multiple times.
 */
interface RepeatablePlugin
