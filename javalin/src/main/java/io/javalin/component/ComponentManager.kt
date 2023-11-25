package io.javalin.component

import io.javalin.http.Context
import java.util.IdentityHashMap
import java.util.function.Consumer
import org.jetbrains.annotations.ApiStatus.Experimental

class ComponentManager {

    private val componentResolvers = IdentityHashMap<ComponentAccessor<*>, ComponentResolver<*>>()
    @Suppress("DEPRECATION")
    private val configurableComponentResolvers = IdentityHashMap<ConfigurableComponentAccessor<*, *>, ConfigurableComponentResolver<*, *>>()

    fun <COMPONENT> register(accessor: ComponentAccessor<COMPONENT>, resolver: ComponentResolver<COMPONENT>) {
        componentResolvers[accessor] = resolver
    }

    fun <COMPONENT> registerIfAbsent(accessor: ComponentAccessor<COMPONENT>, resolver: ComponentResolver<COMPONENT>) {
        componentResolvers.putIfAbsent(accessor, resolver)
    }

    @Experimental
    @Deprecated("Experimental")
    @Suppress("DEPRECATION")
    fun <COMPONENT, CFG> register(accessor: ConfigurableComponentAccessor<COMPONENT, CFG>, resolver: ConfigurableComponentResolver<COMPONENT, CFG>) {
        configurableComponentResolvers[accessor] = resolver
    }

    @Suppress("UNCHECKED_CAST")
    fun <COMPONENT> resolve(accessor: ComponentAccessor<COMPONENT>, ctx: Context?): COMPONENT =
        componentResolvers[accessor]
            ?.resolve(ctx) as COMPONENT
            ?: throw ComponentNotFoundException(accessor)

    @Experimental
    @Deprecated("Experimental")
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    fun <COMPONENT, CFG> resolve(accessor: ConfigurableComponentAccessor<COMPONENT, CFG>, config: Consumer<CFG>, ctx: Context?): COMPONENT =
        configurableComponentResolvers[accessor]
            ?.let { it as ConfigurableComponentResolver<COMPONENT, CFG> }
            ?.let {
                val cfg = accessor.defaultConfig.get()
                config.accept(cfg)
                it.resolve(cfg, ctx)
            }
            ?: throw ComponentNotFoundException(accessor)

}
