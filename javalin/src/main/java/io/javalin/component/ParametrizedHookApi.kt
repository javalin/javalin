package io.javalin.component

import io.javalin.http.Context
import java.util.function.Supplier
import org.jetbrains.annotations.ApiStatus.Experimental

@Deprecated("Experimental")
data class ParametrizedHook<COMPONENT : Any?, PARAMETERS>(
    override val id: String,
    val defaultArguments: Supplier<PARAMETERS>
) : IdentifiableHook

@Deprecated("Experimental")
fun interface ParametrizedResolver<COMPONENT : Any?, CFG> {
    fun resolve(config: CFG, ctx: Context?): COMPONENT
}
