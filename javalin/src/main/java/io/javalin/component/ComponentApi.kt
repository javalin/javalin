package io.javalin.component

import io.javalin.http.Context
import java.util.function.Supplier

open class ComponentAccessor<COMPONENT : Any?> @JvmOverloads constructor(
    val type: Class<COMPONENT>,
    val id: String = type.name
)

class ConfigurableComponentAccessor<COMPONENT : Any?, CFG> @JvmOverloads constructor(
    type: Class<COMPONENT>,
    val defaultConfig: Supplier<CFG>,
    id: String = type.name
) : ComponentAccessor<COMPONENT>(type, id)

fun interface ComponentResolver<COMPONENT : Any?> {
    fun resolve(ctx: Context?): COMPONENT
}

fun interface ConfigurableComponentResolver<COMPONENT : Any?, CFG> {
    fun resolve(config: CFG, ctx: Context?): COMPONENT
}
