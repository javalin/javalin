package io.javalin.component

import io.javalin.http.Context
import java.util.function.Supplier
import org.jetbrains.annotations.ApiStatus.Experimental

open class ComponentAccessor<COMPONENT : Any?>(val id: String)

@Deprecated("Experimental")
@Experimental
class ParametrizedComponentAccessor<COMPONENT : Any?, PARAMETERS>(
    id: String,
    val defaultValues: Supplier<PARAMETERS>
) : ComponentAccessor<COMPONENT>(id)

fun interface ComponentResolver<COMPONENT : Any?> {
    fun resolve(ctx: Context?): COMPONENT
}

@Deprecated("Experimental")
@Experimental
fun interface ParametrizedComponentResolver<COMPONENT : Any?, CFG> {
    fun resolve(config: CFG, ctx: Context?): COMPONENT
}

class ComponentNotFoundException(
    accessor: ComponentAccessor<*>
) : IllegalStateException("No component resolver registered for ${accessor.id}")
