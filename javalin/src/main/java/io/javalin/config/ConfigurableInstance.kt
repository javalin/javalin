package io.javalin.config

fun interface ConfigurableInstance {
    fun cfg(): JavalinConfig
}
