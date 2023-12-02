package io.javalin.component

import io.javalin.http.Context
import java.util.IdentityHashMap
import java.util.function.Consumer
import org.jetbrains.annotations.ApiStatus.Experimental

class ComponentManager {

    private val componentResolvers = IdentityHashMap<ComponentAccessor<*>, ComponentResolver<*>>()
    @Suppress("DEPRECATION")
    private val parametrizedComponentResolvers = IdentityHashMap<ParametrizedComponentAccessor<*, *>, ParametrizedComponentResolver<*, *>>()

    fun <COMPONENT> register(accessor: ComponentAccessor<COMPONENT>, resolver: ComponentResolver<COMPONENT>) {
        componentResolvers[accessor] = resolver
    }

    fun <COMPONENT> registerIfAbsent(accessor: ComponentAccessor<COMPONENT>, component: COMPONENT) {
        registerResolverIfAbsent(accessor) { component }
    }

    fun <COMPONENT> registerResolverIfAbsent(accessor: ComponentAccessor<COMPONENT>, resolver: ComponentResolver<COMPONENT>) {
        componentResolvers.putIfAbsent(accessor, resolver)
    }

    @Experimental
    @Deprecated("Experimental")
    @Suppress("DEPRECATION")
    fun <COMPONENT, CFG> register(accessor: ParametrizedComponentAccessor<COMPONENT, CFG>, resolver: ParametrizedComponentResolver<COMPONENT, CFG>) {
        parametrizedComponentResolvers[accessor] = resolver
    }

    @Suppress("UNCHECKED_CAST")
    fun <COMPONENT> resolve(accessor: ComponentAccessor<COMPONENT>, ctx: Context?): COMPONENT =
        componentResolvers[accessor]
            ?.resolve(ctx) as COMPONENT
            ?: throw ComponentNotFoundException(accessor)

    @Experimental
    @Deprecated("Experimental")
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    fun <COMPONENT, PARAMETERS> resolve(
        accessor: ParametrizedComponentAccessor<COMPONENT, PARAMETERS>,
        userArguments: Consumer<PARAMETERS>,
        ctx: Context?
    ): COMPONENT =
        parametrizedComponentResolvers[accessor]
            ?.let { it as ParametrizedComponentResolver<COMPONENT, PARAMETERS> }
            ?.let {
                val arguments = accessor.defaultValues.get()
                userArguments.accept(arguments)
                it.resolve(arguments, ctx)
            }
            ?: throw ComponentNotFoundException(accessor)

}
