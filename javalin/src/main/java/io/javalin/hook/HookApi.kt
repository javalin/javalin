package io.javalin.hook

import io.javalin.http.Context
import java.util.function.Supplier

interface IdentifiableHook {
    val id: String
}

data class Hook<VALUE : Any?>(
    override val id: String
) : IdentifiableHook

fun interface Resolver<VALUE : Any?> {
    fun resolve(ctx: Context?): VALUE
}

@Deprecated("Experimental")
data class ParametrizedHook<VALUE : Any?, PARAMETERS>(
    override val id: String,
    val defaultArguments: Supplier<PARAMETERS>
) : IdentifiableHook

@Deprecated("Experimental")
fun interface ParametrizedResolver<VALUE : Any?, CFG> {
    fun resolve(config: CFG, ctx: Context?): VALUE
}

class HookNotFoundException(
    hook: IdentifiableHook
) : IllegalStateException("No hook resolver registered for ${hook.id}")

class HooktAlreadyExistsException(
    hook: IdentifiableHook
) : IllegalStateException("Hook resolver already registered for ${hook.id}")
