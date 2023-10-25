package io.javalin.util.mock

fun interface MockConfigurer {
    /** Apply changes to the [MockConfig] instance. */
    fun MockConfig.configure()
}

internal fun invokeMockConfigurerWithAsSamWithReceiver(fn: MockConfigurer, receiver: MockConfig) {
    with(fn) { receiver.configure() }
}
