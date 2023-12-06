package io.javalin.mock

fun interface ContextMockConfigurer {
    /** Apply changes to the [ContextMockConfig] instance. */
    fun ContextMockConfig.configure()
}

internal fun invokeConfigWithConfigurerScope(configurer: ContextMockConfigurer, config: ContextMockConfig) {
    with(configurer) { config.configure() }
}
