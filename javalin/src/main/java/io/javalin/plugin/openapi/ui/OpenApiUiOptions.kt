package io.javalin.plugin.openapi.ui

abstract class OpenApiUiOptions<T : OpenApiUiOptions<T>>(val path: String) {
    abstract val defaultTitle: String
    var title: String? = null

    fun title(value: String) = build { title = value }

    fun createTitle(): String = title ?: defaultTitle

    protected fun build(builder: OpenApiUiOptions<T>.() -> Unit): T {
        this.builder()
        return this as T
    }
}
