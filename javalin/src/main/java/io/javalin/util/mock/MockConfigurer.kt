package io.javalin.util.mock

fun interface MockConfigurer {
    fun MockConfig.configure()
}

internal fun invokeMockConfigurerWithAsSamWithReceiver(fn: MockConfigurer, receiver: MockConfig) {
    with(fn) { receiver.configure() }
}
