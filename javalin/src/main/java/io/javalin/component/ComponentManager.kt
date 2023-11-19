package io.javalin.component

import io.javalin.http.Context
import java.util.IdentityHashMap
import java.util.function.Consumer

class ComponentManager {

    private val componentResolvers = IdentityHashMap<ComponentAccessor<*>, ComponentResolver<*>>()
    private val configurableComponentResolvers = IdentityHashMap<ConfigurableComponentAccessor<*, *>, ConfigurableComponentResolver<*, *>>()

    fun <COMPONENT> registerResolver(accessor: ComponentAccessor<COMPONENT>, resolver: ComponentResolver<COMPONENT>) {
        componentResolvers[accessor] = resolver
    }

    fun <COMPONENT> registerIfAbsent(accessor: ComponentAccessor<COMPONENT>, resolver: ComponentResolver<COMPONENT>) {
        componentResolvers.putIfAbsent(accessor, resolver)
    }

    fun <COMPONENT, CFG> registerResolver(accessor: ConfigurableComponentAccessor<COMPONENT, CFG>, resolver: ConfigurableComponentResolver<COMPONENT, CFG>) {
        configurableComponentResolvers[accessor] = resolver
    }

    @Suppress("UNCHECKED_CAST")
    fun <COMPONENT> resolve(accessor: ComponentAccessor<COMPONENT>, ctx: Context?): COMPONENT =
        componentResolvers[accessor]
            ?.resolve(ctx) as COMPONENT
            ?: throw IllegalStateException("No component resolver registered for ${accessor.id}")

    @Suppress("UNCHECKED_CAST")
    fun <COMPONENT, CFG> resolve(accessor: ConfigurableComponentAccessor<COMPONENT, CFG>, config: Consumer<CFG>, ctx: Context?): COMPONENT =
        configurableComponentResolvers[accessor]
            ?.let { it as ConfigurableComponentResolver<COMPONENT, CFG> }
            ?.let {
                val cfg = accessor.defaultConfig.get()
                config.accept(cfg)
                it.resolve(cfg, ctx)
            }
            ?: throw IllegalStateException("No component resolver registered for ${accessor.id}")

}
