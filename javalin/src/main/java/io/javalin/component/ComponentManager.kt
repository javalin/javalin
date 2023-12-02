@file:Suppress("DEPRECATION")

package io.javalin.component

import io.javalin.http.Context
import java.util.function.Consumer

class ComponentManager {

    private val resolvers: MutableMap<IdentifiableHook, ParametrizedResolver<*, *>> = mutableMapOf()

    fun <COMPONENT> register(hook: Hook<COMPONENT>, resolver: Resolver<COMPONENT>) {
        if (resolvers.containsKey(hook)) {
            throw ComponentAlreadyExistsException(hook)
        }

        resolvers[hook] = ParametrizedResolver<COMPONENT, Unit> { _, ctx ->
            resolver.resolve(ctx)
        }
    }

    @Deprecated("Experimental")
    fun <COMPONENT, CFG> register(hook: ParametrizedHook<COMPONENT, CFG>, resolver: ParametrizedResolver<COMPONENT, CFG>) {
        if (resolvers.containsKey(hook)) {
            throw ComponentAlreadyExistsException(hook)
        }

        resolvers[hook] = resolver
    }

    fun <COMPONENT> registerIfAbsent(hook: Hook<COMPONENT>, component: COMPONENT) {
        registerResolverIfAbsent(hook) { component }
    }

    fun <COMPONENT> registerResolverIfAbsent(hook: Hook<COMPONENT>, resolver: Resolver<COMPONENT>) {
        resolvers.putIfAbsent(hook, ParametrizedResolver<COMPONENT, Unit> { _, ctx ->
            resolver.resolve(ctx)
        })
    }

    fun <COMPONENT> resolve(hook: Hook<COMPONENT>, ctx: Context?): COMPONENT =
        resolvers[hook]
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it as ParametrizedResolver<COMPONENT, Unit>
            }
            ?.resolve(Unit, ctx)
            ?: throw ComponentNotFoundException(hook)

    @Deprecated("Experimental")
    fun <COMPONENT, PARAMETERS> resolve(hook: ParametrizedHook<COMPONENT, PARAMETERS>, userArguments: Consumer<PARAMETERS>, ctx: Context?): COMPONENT =
        resolvers[hook]
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it as ParametrizedResolver<COMPONENT, PARAMETERS>
            }
            ?.let {
                val arguments = hook.defaultArguments.get()
                userArguments.accept(arguments)
                it.resolve(arguments, ctx)
            }
            ?: throw ComponentNotFoundException(hook)

}
