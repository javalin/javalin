package io.javalin.router

import io.javalin.config.JavalinState

interface RoutingApi

fun interface RoutingApiInitializer<SETUP> {
    fun initialize(cfg: JavalinState, internalRouter: InternalRouter, setup: RoutingSetupScope<SETUP>)
}

fun interface RoutingSetupScope<SETUP> {
    fun SETUP.setup()
}

internal fun <SETUP> RoutingSetupScope<SETUP>.invokeAsSamWithReceiver(receiver: SETUP) {
    with(this) { receiver.setup() }
}
