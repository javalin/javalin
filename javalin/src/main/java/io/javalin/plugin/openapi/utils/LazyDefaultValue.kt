package io.javalin.plugin.openapi.utils

import kotlin.reflect.KProperty

internal class LazyDefaultValue<T>(private val init: () -> T) {
    var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        value = value ?: init()
        return value!!
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
