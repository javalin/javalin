package io.javalin.mock

fun interface ContextMockConfigurer {
    /** Apply changes to the [ContextMockConfig] instance. */
    fun ContextMockConfig.configure()
}

internal fun invokeMockConfigurerWithAsSamWithReceiver(fn: ContextMockConfigurer, receiver: ContextMockConfig) {
    with(fn) { receiver.configure() }
}
