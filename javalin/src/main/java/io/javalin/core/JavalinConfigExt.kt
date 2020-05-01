package io.javalin.core

import io.javalin.core.plugin.Plugin

inline fun <reified T : Plugin> JavalinConfig.getPlugin(): T = getPlugin(T::class.java)
