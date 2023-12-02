@file:Suppress("DEPRECATION")

package io.javalin.hook

import io.javalin.http.Context
import java.util.function.Consumer

class HookManager {

    private val resolvers: MutableMap<IdentifiableHook, ParametrizedResolver<*, *>> = mutableMapOf()

    fun <VALUE> register(hook: Hook<VALUE>, resolver: Resolver<VALUE>) {
        if (resolvers.containsKey(hook)) {
            throw HooktAlreadyExistsException(hook)
        }

        resolvers[hook] = ParametrizedResolver<VALUE, Unit> { _, ctx ->
            resolver.resolve(ctx)
        }
    }

    @Deprecated("Experimental")
    fun <VALUE, CFG> register(hook: ParametrizedHook<VALUE, CFG>, resolver: ParametrizedResolver<VALUE, CFG>) {
        if (resolvers.containsKey(hook)) {
            throw HooktAlreadyExistsException(hook)
        }

        resolvers[hook] = resolver
    }

    fun <VALUE> registerIfAbsent(hook: Hook<VALUE>, component: VALUE) {
        registerResolverIfAbsent(hook) { component }
    }

    fun <VALUE> registerResolverIfAbsent(hook: Hook<VALUE>, resolver: Resolver<VALUE>) {
        resolvers.putIfAbsent(hook, ParametrizedResolver<VALUE, Unit> { _, ctx ->
            resolver.resolve(ctx)
        })
    }

    @JvmOverloads
    fun <VALUE> resolve(hook: Hook<VALUE>, ctx: Context? = null): VALUE =
        resolvers[hook]
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it as ParametrizedResolver<VALUE, Unit>
            }
            ?.resolve(Unit, ctx)
            ?: throw HookNotFoundException(hook)

    @Deprecated("Experimental")
    @JvmOverloads
    fun <VALUE, PARAMETERS> resolve(hook: ParametrizedHook<VALUE, PARAMETERS>, userArguments: Consumer<PARAMETERS>, ctx: Context? = null): VALUE =
        resolvers[hook]
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it as ParametrizedResolver<VALUE, PARAMETERS>
            }
            ?.let {
                val arguments = hook.defaultArguments.get()
                userArguments.accept(arguments)
                it.resolve(arguments, ctx)
            }
            ?: throw HookNotFoundException(hook)

}
