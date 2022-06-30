package io.javalin.core

import io.javalin.core.plugin.Plugin

@JvmSynthetic
inline fun <reified T : Plugin> JavalinConfig.getPlugin(): T = getPlugin(T::class.java)
