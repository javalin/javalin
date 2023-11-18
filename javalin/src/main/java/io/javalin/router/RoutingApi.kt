package io.javalin.router

import io.javalin.config.JavalinConfig

interface RoutingApi

fun interface RoutingApiInitializer<SETUP> {
    fun initialize(cfg: JavalinConfig, internalRouter: InternalRouter, setup: RoutingSetupScope<SETUP>)
}

fun interface RoutingSetupScope<SETUP> {
    fun SETUP.setup()
}

internal fun <SETUP> RoutingSetupScope<SETUP>.invokeWithAsSamWithReceiver(receiver: SETUP) {
    with(this) { receiver.setup() }
}
